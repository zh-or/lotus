package or.lotus.common.http.server;

import or.lotus.common.http.server.exception.HttpRequestPathNotFoundException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    HttpServer server;

    public HttpServerHandler(HttpServer server) {
        this.server = server;
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        HttpRequestPkg req = new HttpRequestPkg(ctx, server, request);
        String path = req.getPath();
        try {
            Enumeration<String> keys = server.getServices().keys();
            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                if(path.startsWith(key)) {
                    HttpServiceWrap serviceWrap = server.getServices().get(key);
                    if(serviceWrap != null) {
                        if(server.eventExec != null) {
                            //用其他线程时才需要重新引用
                            req.retain();
                        }
                        server.exec(() -> {
                            String newPath = path.toString();
                            try {
                                ArrayList<HttpRestServiceHook> hooks = server.getHooks();
                                for (HttpRestServiceHook filter : hooks) {
                                    Object interceptRes = filter.requestHook(req);
                                    if (interceptRes != null) {
                                        sendResponse(ctx, req, interceptRes);
                                        return;
                                    }
                                }
                            } catch (Exception e) {
                                sendResponse(ctx, req, server.exception(e, req));
                                return;
                            }
                            if(!"/".equals(key)) {
                                newPath = newPath.replaceFirst(key, "");
                            }
                            boolean isHandle = false;
                            for(HttpBaseService service : serviceWrap.services) {
                                try {
                                    Object res = service.__dispatch(newPath, req);
                                    sendResponse(ctx, req, res);
                                    isHandle = true;
                                    req.release();
                                    break;
                                } catch (HttpRequestPathNotFoundException e2) {
                                } finally {
                                    if(server.eventExec!= null) {
                                        //手动调用会报错
                                        //req.release();
                                    }
                                }
                            }
                            if(!isHandle) {
                                //没有匹配到 controller, 走文件处理
                                ctx.fireChannelRead(req.rawRequest);
                            }
                        });
                        return;
                    }
                }
            }

        } catch(Throwable e) {
            e.printStackTrace();
        }
        ctx.fireChannelRead(request.retain());

       /* System.out.println(channel.remoteAddress()); // 显示客户端的远程地址
        String content = String.format("Receive http request, uri: %s, method: %s, content: %s%n", request.uri(), request.method(), request.content().toString(CharsetUtil.UTF_8));
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);*/

    }


    private boolean sendResponse(ChannelHandlerContext ctx, HttpRequestPkg request, Object res) {
        FullHttpResponse response = null;
        if(res == null) {
            response = HttpResponsePkg.create().raw();
        } else if(res instanceof HttpResponsePkg) {
            response = ((HttpResponsePkg) res).raw();
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
                    return sendResponse(ctx, request, res2);
                }

                if(server.outModelAndViewTime) {
                    try {
                        writer.write("<!-- handle time: " + (System.currentTimeMillis() - mv.createTime) + "ms -->");
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
}
