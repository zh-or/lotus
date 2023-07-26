package lotus.http.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.ArrayList;
import java.util.Enumeration;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;

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
                    HttpBaseService service = server.getServices().get(key);
                    if(service != null) {
                        req.retain();
                        server.exec(() -> {
                            try {
                                ArrayList<HttpRestServiceHook> hooks = server.getFilters();
                                for(HttpRestServiceHook filter : hooks) {
                                    Object interceptRes = filter.requestHook(req);
                                    if(interceptRes != null) {
                                        sendResponse(ctx, req, interceptRes);
                                        return ;
                                    }
                                }

                                Object res = service.__dispatch(path.replace(key, ""), req);

                                sendResponse(ctx, req, res);
                            } catch (RequestPathNotFound e2) {
                                /*不处理, 走后面的资源处理handler*/
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
        } else {
            response = HttpResponsePkg.create(request, res.toString()).raw();
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE,
                "text/plain; charset=" + server.charset.displayName());

        final boolean keepAlive = HttpUtil.isKeepAlive(request.rawRequest);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        if (!keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.rawRequest.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ArrayList<HttpRestServiceHook> hooks = server.getFilters();
        for(HttpRestServiceHook filter : hooks) {
            filter.responseHook(request, response);
        }
        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }

        return true;
    }
}
