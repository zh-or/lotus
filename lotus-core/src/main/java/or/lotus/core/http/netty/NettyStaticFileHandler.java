package or.lotus.core.http.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.support.RestfulUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyStaticFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    NettyHttpServer server;

    public NettyStaticFileHandler(NettyHttpServer server) {
        this.server = server;
    }


    private String sanitizeUri(String uri) {
        if(this.server.staticPath == null) {
            return null;
        }
        if (uri == null || uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        try {
            URI uri2 = new URI(uri);
            uri = URLDecoder.decode(uri2.getPath(), this.server.getCharset().displayName());
            uri = uri.replace('\\', '/');
            if("/".equals(uri)) {
                uri = this.server.defaultIndexFile;
            }

            return this.server.staticPath + File.separator +  Utils.BuildPath(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if(this.server.fileFilter != null && this.server.fileFilter.before(request)) {
            sendError(ctx, request, NOT_FOUND);
            return;
        }

        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, request, BAD_REQUEST);
            return;
        }

        if (!GET.equals(request.method())) {
            sendError(ctx, request, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = request.uri();
        final String path = sanitizeUri(uri);

        if (path == null) {
            sendError(ctx, request, FORBIDDEN);
            return;
        }

        //支持符号链接
        Path p2 = Paths.get(path);

        File file = null;
        if(Files.isSymbolicLink(p2)) {
            /**检查配置是否启用支持软链接*/
            if(!server.isSupportSymbolicLink) {
                sendError(ctx, request, NOT_FOUND);
                return ;
            }

            p2 = Files.readSymbolicLink(p2);
            file = p2.toFile();
        } else {
            file = new File(path);
        }

        if(file.isHidden()) {
            sendError(ctx, request, NOT_FOUND);
            return ;
        } else if(!file.exists()) {
            //自动加上 html 结尾试试
            if(file.getName().lastIndexOf(".") == -1) {
                file = new File(path + ".html");
            }
        }

        if (file.isHidden() || !file.exists()) {
            sendError(ctx, request, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            sendError(ctx, request, NOT_FOUND);
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, request, FORBIDDEN);
            return;
        }
        final boolean keepAlive = HttpUtil.isKeepAlive(request);

        // Cache Validation
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (!Utils.CheckNull(ifModifiedSince)) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.CHINA);
            Date ifModifiedSinceDate = null;
            try {
                ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    sendNotModified( ctx, request);
                    return;
                }
            } catch (ParseException e) {
                //格式化时间出错
            }
        }

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

        if(this.server.fileFilter != null && this.server.fileFilter.request(file, request, response)) {

            ChannelFuture flushPromise = ctx.writeAndFlush(response);
            if(!keepAlive) {
                flushPromise.addListener(ChannelFutureListener.CLOSE);
            }
            return;
        }

        RandomAccessFile raf;
        long fileLength = 0;
        try {
            raf = new RandomAccessFile(file, "r");
            fileLength = raf.length();
        } catch (Exception ignore) {
            sendError(ctx, request, NOT_FOUND);
            return;
        }
        HttpUtil.setContentLength(response, fileLength);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, RestfulUtils.getMimeType(this.server.getCharset().displayName(), file));

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
        headers.set(HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(file.lastModified())));
        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

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
    }

    public void sendNotModified(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);

        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        sendAndCleanupConnection(ctx, request, response);
    }

    private void sendError(ChannelHandlerContext ctx, final FullHttpRequest request, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", server.getCharset()));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=" + server.getCharset().displayName());
        sendAndCleanupConnection(ctx, request, response);
    }

    private void sendAndCleanupConnection(ChannelHandlerContext ctx, final FullHttpRequest request, FullHttpResponse response) {
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
}
