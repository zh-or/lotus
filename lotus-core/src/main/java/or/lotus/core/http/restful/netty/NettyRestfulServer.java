package or.lotus.core.http.restful.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.support.RestfulResponseStatus;

import java.io.File;

public class NettyRestfulServer extends RestfulContext {

    int responseBufferSize = 1024 * 4;
    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    ServerBootstrap serverBootstrap = null;
    SslContext sslContext = null;


    public void setKeyStoreAndEnableSSL(String keystore, String password) throws Exception {
        sslContext = SslContextBuilder.forServer(new File(keystore), null, password).build();
    }

    @Override
    protected void onStart() throws InterruptedException {
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

                pipeline.addLast("apiHandler", new NettyRequestHandler(NettyRestfulServer.this));//处理api 请求
                //pipeline.addLast("staticFileHandler", new HttpStaticFileServerHandler(HttpServer.this));//处理静态文件请求
            }

        });
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        Channel ch = serverBootstrap.bind(bindAddress).sync().channel();

    }

    private class NettyRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        NettyRestfulServer context;

        public NettyRequestHandler(NettyRestfulServer context) {
            this.context = context;
        }

        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

            NettyRequest request = new NettyRequest(NettyRestfulServer.this, ctx, msg);
            NettyResponse response = new NettyResponse(request);
            request.retain();
            context.dispatch(request, response);
        }
    }



    @Override
    protected void onStop() {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (Exception e) {
            log.error("停止服务器失败:", e);
        }
    }

    @Override
    protected void sendResponse(boolean isHandle, RestfulRequest _request, RestfulResponse _response) {

        NettyRequest request = (NettyRequest) _request;
        NettyResponse response = (NettyResponse) _response;

        if(!isHandle) {
            //SimpleChannelInboundHandler 会自动释放如果不增加引用到下一个静态文件处理器就炸了
            //fireChannelRead不会增加引用计数
            //暂时返回404
            response.setStatus(RestfulResponseStatus.CLIENT_ERROR_NOT_FOUND);
            request.channel.writeAndFlush(response.getResponse());
            request.release();
            /*request.channel.fireChannelRead(request.retain());
            request.release();*/
            return;
        }

        request.channel.writeAndFlush(response.getResponse());
        request.release();
    }

}
