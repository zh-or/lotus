package lotus.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.File;
import java.net.InetSocketAddress;

public class HttpServer {
    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    ServerBootstrap serverBootstrap = null;
    SslContext sslContext = null;

    public HttpServer() {

    }

    public void setKeyStoreAndEnableSSL(String keystore, String password) throws Exception {
        sslContext = SslContextBuilder.forServer(new File(keystore), null, password).build();
    }

    public void start() {
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);
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
                        pipeline.addLast("codec", new HttpServerCodec());// HTTP 编解码
                        pipeline.addLast("compressor", new HttpContentCompressor());// HttpContent 压缩
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536)); // HTTP 消息聚合
                        pipeline.addLast("handler", new HttpServerHandler(HttpServer.this));

                    }
                });
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    }

    public void stop() {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (Exception e) {

        }
    }

    public void bind(InetSocketAddress address) throws InterruptedException {

        Channel ch = serverBootstrap.bind(address).sync().channel();
    }
}
