package or.lotus.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import or.lotus.common.GraceShutdown;
import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class LotusResponseSender {
    static Logger log = LoggerFactory.getLogger(LotusResponseSender.class);
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");
    public static final int HTTP_CACHE_SECONDS = 60;

    /**根据 res 的类型选择返回方式*/
    public static boolean sendResponse(HttpServer server, ChannelHandlerContext ctx, HttpRequestPkg request, Object res) {
        FullHttpResponse response = null;
        if(res == null) {
            response = HttpResponsePkg.create().raw();
        } else if(res instanceof HttpResponsePkg) {
            HttpResponsePkg resPkg = ((HttpResponsePkg) res);
            if(resPkg.isFileResponse()) {
                try {
                    sendFile(server, ctx, request.rawRequest, resPkg.file.getAbsolutePath());
                    return true;
                } catch (IOException e) {
                    log.error("发送文件出错:", e);
                }
            }

            response = resPkg.raw();


        } else if(res instanceof ModelAndView) {
            if(server.templateEngine == null) {
                throw new IllegalStateException("你返回了ModelAndView, 但是并没有启用模板引擎.");
            }
            ModelAndView mv = (ModelAndView) res;
            if(mv.isRedirect) {//302跳转
                response = new DefaultFullHttpResponse(HTTP_1_1, FOUND, Unpooled.EMPTY_BUFFER);
                response.headers().set(HttpHeaderNames.LOCATION, mv.getViewName());
            } else {
                TemplateWriter writer = new TemplateWriter(request.channelCtx.alloc().heapBuffer(server.responseBufferSize), server.charset);
                try {
                    server.templateEngine.process(
                            mv.getViewName(),
                            mv.values,
                            writer
                    );
                } catch(Exception e) {
                    Object res2 = server.exception(e, request);
                    return sendResponse(server, ctx, request, res2);
                }

                if(server.outModelAndViewTime) {
                    try {
                        writer.write("<!-- handle time: " + ((System.nanoTime() - mv.createTime) / 1_000_000) + "ms -->");
                    } catch (IOException e) {}
                }

                response = HttpResponsePkg.create(
                        request,
                        writer.getBuffer()
                ).raw();
                //todo 此处需要根据实际情况设置缓存头
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=" + server.charset.displayName());
            }

        } else {
            response = HttpResponsePkg.create(request, res.toString()).raw();
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=" + server.charset.displayName());
        }

        final boolean keepAlive = HttpUtil.isKeepAlive(request.rawRequest);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        if (!keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.rawRequest.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ArrayList<HttpRestServiceHook> hooks = server.getHooks();
        for(HttpRestServiceHook filter : hooks) {
            filter.responseHook(request, response);
        }
        //todo 需要判断此时是否可写, 如果写入速度过快则会资源耗尽
        //ctx.channel().isWritable();

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }

        return true;
    }

    public static void sendFile(HttpServer context, ChannelHandlerContext ctx, final FullHttpRequest request, String path) throws IOException {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);

        //支持符号链接
        Path p2 = Paths.get(path);

        File file = null;
        if(Files.isSymbolicLink(p2)) {
            p2 = Files.readSymbolicLink(p2);
            file = p2.toFile();
        } else {
            file = new File(path);
        }
        final String uri = request.uri();

        if(file.isHidden()) {
            sendError(context, ctx, request, NOT_FOUND);
            return ;
        } else if(!file.exists()) {
            //自动加上 html 结尾试试
            if(file.getName().lastIndexOf(".") == -1) {
                file = new File(path + ".html");
            }
        }

        if (file.isHidden() || !file.exists()) {
            sendError(context, ctx, request, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                if(context.isEnableDirList()) {
                    sendListing(context, ctx,  request, file, uri);
                } else {
                    sendError(context, ctx, request, NOT_FOUND);
                }
            } else {
                sendRedirect(context, ctx, request, uri + '/');
            }
            return;
        }

        if (!file.isFile()) {
            sendError(context, ctx, request, FORBIDDEN);
            return;
        }

        HttpStaticFileHook fileHook = context.getStaticFileHook();
        if(fileHook != null) {
            Object obj = fileHook.requestHook(request, file);
            if(obj != null) {
                sendString(context, ctx, request, obj.toString());
                return;
            }
        }

        // Cache Validation
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (!Utils.CheckNull(ifModifiedSince)) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = null;
            try {
                ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
                // Only compare up to the second because the datetime format we send to the client
                // does not have milliseconds
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    sendNotModified(context, ctx, request);
                    return;
                }
            } catch (ParseException e) {
                //格式化时间出错
            }
        }

        RandomAccessFile raf;
        long fileLength = 0;
        try {
            raf = new RandomAccessFile(file, "r");
            fileLength = raf.length();
        } catch (Exception ignore) {
            sendError(context, ctx, request, NOT_FOUND);
            return;
        }

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        ArrayList<HttpRestServiceHook> hooks = context.getHooks();

        if(fileHook != null) {
            fileHook.responseHook(request, response);
        }

        HttpUtil.setContentLength(response, fileLength);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, getMimeType(context.charset.displayName(), file));

        setDateAndCacheHeaders(response, file);

        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        } else {
            sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

/*        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });*/

        // Decide whether to close the connection or not.
        if (!keepAlive) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        //todo 不知道为什么会有释放, 关闭后netty会再释放一次, 就报错了
        //request.release();
    }

    public static void sendString(HttpServer server, ChannelHandlerContext ctx, final FullHttpRequest request, String str) {

        ByteBuf buffer = ctx.alloc().buffer(str.length());
        buffer.writeCharSequence(str, CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=" + server.charset.displayName());

        sendAndCleanupConnection(server, ctx, request, response);
    }

    public static void sendListing(HttpServer server, ChannelHandlerContext ctx, final FullHttpRequest request, File dir, String dirPath) {
        StringBuilder buf = new StringBuilder()
                .append("<!DOCTYPE html>\r\n")
                .append("<html><head><meta charset='" + server.charset.displayName() + "' /><title>")
                .append("Listing of: ")
                .append(dirPath)
                .append("</title></head><body>\r\n")

                .append("<h3>Listing of: ")
                .append(dirPath)
                .append("</h3>\r\n")

                .append("<ul>")
                .append("<li><a href=\"../\">..</a></li>\r\n");

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f: files) {
                if (f.isHidden() || !f.canRead()) {
                    continue;
                }

                String name = f.getName();
                if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                    continue;
                }

                buf.append("<li><a href=\"")
                        .append(name)
                        .append("\">")
                        .append(name)
                        .append("</a></li>\r\n");
            }
        }

        buf.append("</ul></body></html>\r\n");

        ByteBuf buffer = ctx.alloc().buffer(buf.length());
        buffer.writeCharSequence(buf.toString(), CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=" + server.charset.displayName());

        sendAndCleanupConnection(server, ctx, request, response);
    }

    public static void sendRedirect(HttpServer server, ChannelHandlerContext ctx, final FullHttpRequest request, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND, Unpooled.EMPTY_BUFFER);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        sendAndCleanupConnection(server, ctx, request, response);
    }

    public static void sendError(HttpServer server, ChannelHandlerContext ctx, final FullHttpRequest request, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=" + server.charset.displayName());

        sendAndCleanupConnection(server, ctx, request, response);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx
     *            Context
     */
    public static  void sendNotModified(HttpServer server, ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
        setDateHeader(response);

        sendAndCleanupConnection(server, ctx, request, response);
    }

    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    public static void sendAndCleanupConnection(HttpServer server, ChannelHandlerContext ctx, final FullHttpRequest request, FullHttpResponse response) {
        HttpUtil.setContentLength(response, response.content().readableBytes());
        boolean keepAlive = false;
        if(request != null) {
            keepAlive = HttpUtil.isKeepAlive(request);
            if (!keepAlive) {
                // We're going to close the connection as soon as the response is sent,
                // so we should also make it clear for the client.
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                // Close the connection as soon as the response is sent.
            } else if (request.protocolVersion().equals(HTTP_1_0)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);
        if(!keepAlive) {
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response
     *            HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        HttpHeaders headers = response.headers();
        headers.set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        headers.set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        headers.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        headers.set(HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    public static String getMimeType(String charset, File fileUrl) {

        //String type = URLConnection.guessContentTypeFromName(fileUrl.getPath());
        String type = null;
        try {
            type = Files.probeContentType(fileUrl.toPath());
        } catch (IOException e) {
        }
        if(type == null) {
            if(fileUrl.toPath().endsWith(".js")) {
                return "text/javascript; charset=" + charset;
            }
            return "text/html; charset=" + charset;
        }
        return type;
    }
}
