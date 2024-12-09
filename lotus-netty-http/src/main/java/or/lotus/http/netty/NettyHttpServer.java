package or.lotus.http.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.AttributeKey;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.support.RestfulUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class NettyHttpServer extends RestfulContext {

    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    ServerBootstrap serverBootstrap = null;
    SslContext sslContext = null;
    NettyFileFilter fileFilter = null;
    HashMap<String, NettyWebSocketMessageHandler> webSocketHandlers = new HashMap<>();

    String defaultIndexFile = "index.html";
    String staticPath = null;
    boolean isSupportSymbolicLink = false;

    public void setKeyStoreAndEnableSSL(String keystore, String password) throws Exception {
        sslContext = SslContextBuilder.forServer(new File(keystore), null, password).build();
    }

    /** 会在调用start方法时向filter内注入bean */
    public void setFileFilter(NettyFileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public void addWebSocketMessageHandler(NettyWebSocketMessageHandler webSocketMessageHandler) {
        tmpControllers.add(webSocketMessageHandler);
        webSocketHandlers.put(webSocketMessageHandler.getPath(), webSocketMessageHandler);
    }

    public void setDefaultIndexFile(String defaultIndexFile) {
        this.defaultIndexFile = defaultIndexFile;
    }

    public void setStaticPath(String staticPath) {
        this.staticPath = staticPath;
    }

    public void setSupportSymbolicLink(boolean supportSymbolicLink) {
        isSupportSymbolicLink = supportSymbolicLink;
    }

    @Override
    protected void onStart() throws InterruptedException {

        try {
            RestfulUtils.injectBeansToObject(this, fileFilter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        //accept thread group
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);
        //io thread group
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        //serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                if (sslContext != null) {
                    pipeline.addLast(sslContext.newHandler(socketChannel.alloc()));
                }
                pipeline.addLast("httpCodec", new HttpServerCodec());// HTTP 编解码
                if(isEnableGZIP) {
                    pipeline.addLast("decompressor", new HttpContentDecompressor());// HttpContent 解压缩
                    pipeline.addLast("compressor", new HttpContentCompressor());// HttpContent 压缩
                }
                pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength)); // HTTP 消息聚合

                if(webSocketHandlers.size() > 0) {
                    //pipeline.addLast("websocketDecoder", new WebSocketServerProtocolHandler());//处理websocket请求
                    pipeline.addLast("websocketCompressor", new WebSocketServerCompressionHandler());

                    for(Map.Entry<String, NettyWebSocketMessageHandler> entry : webSocketHandlers.entrySet()) {
                        WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                                .checkStartsWith(true)//值检查url的前面是否匹配, 不然的话不能在url上传参数
                                .allowExtensions(true)
                                .handleCloseFrames(true)
                                //.allowMaskMismatch(false)//mask
                                .websocketPath(entry.getValue().getPath())
                                .build();

                        //处理websocket请求
                        pipeline.addLast("websocketDecoder-" + entry.getKey(), new WebSocketServerProtocolHandler(config));
                    }
                    pipeline.addLast("websocketHandler", new NettyWebSocketServerHandler(NettyHttpServer.this));//处理websocket请求
                }

                pipeline.addLast("apiHandler", new NettyRequestHandler(NettyHttpServer.this));//处理api 请求
                pipeline.addLast("staticFileHandler", new NettyStaticFileHandler(NettyHttpServer.this));//处理静态文件请求
            }

        });
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        Channel ch = serverBootstrap.bind(bindAddress).sync().channel();

    }


    /** 处理restful请求 */
    private class NettyRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        NettyHttpServer context;

        public NettyRequestHandler(NettyHttpServer context) {
            this.context = context;
        }

        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

            NettyRequest request = new NettyRequest(NettyHttpServer.this, ctx, msg);
            NettyResponse response = new NettyResponse(request);
            request.retain();
            context.dispatch(request, response);
        }
    }

    /** 处理websocket请求 */
    private class NettyWebSocketServerHandler  extends SimpleChannelInboundHandler<WebSocketFrame> {
        final AttributeKey<NettyWebSocketSession> SESSION_ATTRIBUTE_KEY = AttributeKey.valueOf(NettyWebSocketSession.class, "WebSocketSession");
        final AttributeKey<WebSocketServerProtocolHandler.HandshakeComplete> HANDSHAKER_ATTR_KEY = AttributeKey.valueOf(WebSocketServerProtocolHandler.HandshakeComplete.class, "HANDSHAKER");
        NettyHttpServer server;

        public NettyWebSocketServerHandler(NettyHttpServer server) {
            this.server = server;
        }

        public void exec(Runnable run) {
            if(server.executorService != null) {
                server.executorService.execute(run);
            } else {
                run.run();
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                WebSocketServerProtocolHandler.HandshakeComplete info = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
                QueryStringDecoder qsd = new QueryStringDecoder(info.requestUri());

                NettyWebSocketMessageHandler handler = webSocketHandlers.get(qsd.path());
                NettyWebSocketSession session = new NettyWebSocketSession(qsd, info.requestHeaders(), handler, ctx);

                ctx.attr(SESSION_ATTRIBUTE_KEY).set(session);

                exec(() -> {
                    try {
                        handler.onConnection(session);
                    } catch (Exception e) {
                        handler.onException(ctx, e);
                    }
                });

            }

        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            NettyWebSocketSession session = ctx.attr(SESSION_ATTRIBUTE_KEY).get();
            if(session != null) {
                exec(() -> {
                    try {
                        session.getHandler().onClose(session);
                    } catch (Exception e) {
                        session.getHandler().onException(ctx, e);
                    }
                });
            }

        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

            NettyWebSocketSession session = ctx.attr(SESSION_ATTRIBUTE_KEY).get();
            if(session == null) {
                throw new Exception("错误的连接到了错误的handler:" + frame.toString());
            }
            WebSocketFrame newFrame = frame.retain();

            exec(() -> {
                NettyWebSocketMessageHandler handler = session.getHandler();
                try {
                    if(newFrame instanceof CloseWebSocketFrame) {
                        handler.onClose(session);
                    } else if(newFrame instanceof TextWebSocketFrame) {
                        handler.onMessage(session, ((TextWebSocketFrame) newFrame).text());
                    } else if(newFrame instanceof ContinuationWebSocketFrame) {
                        handler.onContinuationMessage(session, (ContinuationWebSocketFrame) newFrame);
                    } else if(newFrame instanceof BinaryWebSocketFrame) {
                        handler.onBinaryMessage(session, (BinaryWebSocketFrame) newFrame);
                    } else if(newFrame instanceof PingWebSocketFrame) {
                        handler.onPing(session, ((PingWebSocketFrame) newFrame));
                    }
                } catch(Exception e) {
                    handler.onException(ctx, e);
                } finally {
                    frame.release();
                }
            });
        }


        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }


    }

    @Override
    protected void onStop() {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (Exception e) {
            RestfulContext.log.error("停止服务器失败:", e);
        }
    }

    @Override
    protected void sendResponse(boolean isHandle, RestfulRequest _request, RestfulResponse _response) {
        NettyRequest request = (NettyRequest) _request;
        NettyResponse response = (NettyResponse) _response;
        if(!isHandle) {
            //SimpleChannelInboundHandler 会自动释放如果不增加引用到下一个静态文件处理器就炸了
            //fireChannelRead不会增加引用计数
            request.channel.fireChannelRead(request.rawRequest().retain());
            /*
            //暂时返回404
            *  response.setStatus(RestfulResponseStatus.CLIENT_ERROR_NOT_FOUND);
            try {
                response.write("<html>\n" +
                        "<head><title>404 Not Found</title></head>\n" +
                        "<body>\n" +
                        "<center><h1>404 Not Found</h1></center>\n" +
                        "<hr><center>" + RestfulContext.TAG + "</center>\n" +
                        "</body>\n" +
                        "</html>");
            } catch(Exception e) {}
            * */
        } else if(response.isFileResponse()) {
            NettyStaticFileHandler.sendFile(response.getFile(), request.rawRequest(), response.getResponse(), request.channel, charset);
        } else {
            FullHttpResponse rawRequest = (FullHttpResponse) response.getResponse();
            final boolean keepAlive = HttpUtil.isKeepAlive(rawRequest);
            HttpUtil.setContentLength(rawRequest, rawRequest.content().readableBytes());
            if (keepAlive) {
                rawRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            } else if (rawRequest.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
                rawRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            }
            ChannelFuture flushPromise = request.channel.writeAndFlush(rawRequest);

            if (!keepAlive) {
                flushPromise.addListener(ChannelFutureListener.CLOSE);
            }
        }

        request.release();
    }
}
