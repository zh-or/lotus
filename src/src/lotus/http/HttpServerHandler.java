package lotus.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    HttpServer server;

    public HttpServerHandler(HttpServer server) {
        this.server = server;
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Channel channel = ctx.channel(); // 获取通道
        System.out.println(channel.remoteAddress()); // 显示客户端的远程地址
        String content = String.format("Receive http request, uri: %s, method: %s, content: %s%n", request.uri(), request.method(), request.content().toString(CharsetUtil.UTF_8));
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }
}
