package or.lotus.core.http.server;

import or.lotus.core.http.server.exception.HttpRequestPathNotFoundException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;


import java.util.ArrayList;
import java.util.Enumeration;

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
                                        LotusResponseSender.sendResponse(server, ctx, req, interceptRes);
                                        req.release();
                                        return;
                                    }
                                }
                            } catch (Exception e) {
                                LotusResponseSender.sendResponse(server, ctx, req, server.exception(e, req));
                                req.release();
                                return;
                            }
                            if(!"/".equals(key)) {
                                newPath = newPath.replaceFirst(key, "");
                            }
                            boolean isHandle = false;
                            for(HttpBaseService service : serviceWrap.services) {
                                try {
                                    Object res = service.__dispatch(newPath, req);
                                    LotusResponseSender.sendResponse(server, ctx, req, res);
                                    isHandle = true;
                                    break;
                                } catch (HttpRequestPathNotFoundException e2) {

                                }
                            }
                            if(!isHandle) {
                                //没有匹配到 controller, 走文件处理
                                ctx.fireChannelRead(req.rawRequest.retain());
                            }
                            req.release();
                        });
                        return;
                    }
                }
            }

        } catch(Throwable e) {
            e.printStackTrace();
        }
        //SimpleChannelInboundHandler 会自动释放如果不增加引用到下一个静态文件处理器就炸了
        //fireChannelRead不会增加引用计数
        ctx.fireChannelRead(request.retain());
        req.release();

       /* System.out.println(channel.remoteAddress()); // 显示客户端的远程地址
        String content = String.format("Receive http request, uri: %s, method: %s, content: %s%n", request.uri(), request.method(), request.content().toString(CharsetUtil.UTF_8));
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);*/

    }



}
