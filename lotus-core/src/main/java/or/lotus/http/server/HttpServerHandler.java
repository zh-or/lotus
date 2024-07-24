package or.lotus.http.server;

import or.lotus.http.server.exception.HttpRequestPathNotFoundException;
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
import static or.lotus.http.server.LotusResponseSender.sendResponse;

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
                                        sendResponse(server, ctx, req, interceptRes);
                                        return;
                                    }
                                }
                            } catch (Exception e) {
                                sendResponse(server, ctx, req, server.exception(e, req));
                                return;
                            }
                            if(!"/".equals(key)) {
                                newPath = newPath.replaceFirst(key, "");
                            }
                            boolean isHandle = false;
                            for(HttpBaseService service : serviceWrap.services) {
                                try {
                                    Object res = service.__dispatch(newPath, req);
                                    sendResponse(server, ctx, req, res);
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
        //ctx.fireChannelRead(request.retain());//
        ctx.fireChannelRead(request);//不需要调用retain?

       /* System.out.println(channel.remoteAddress()); // 显示客户端的远程地址
        String content = String.format("Receive http request, uri: %s, method: %s, content: %s%n", request.uri(), request.method(), request.content().toString(CharsetUtil.UTF_8));
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);*/

    }



}
