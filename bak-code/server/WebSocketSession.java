package or.lotus.core.http.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketSession {
    private QueryStringDecoder qsd;
    private String path;
    private WebSocketMessageHandler handler;
    private ChannelHandlerContext ctx;
    private HttpHeaders requestHeaders;

    public WebSocketSession(QueryStringDecoder qsd, HttpHeaders headers, WebSocketMessageHandler handler, ChannelHandlerContext ctx) {
        this.qsd = qsd;
        this.requestHeaders = headers;
        this.path = qsd.path();
        this.handler = handler;
        this.ctx = ctx;
    }

    public HttpHeaders getRequestHeaders() {
        return requestHeaders;
    }


    public QueryStringDecoder getQueryStringDecoder() {
        return qsd;
    }

    public String getPath() {
        return path;
    }

    public WebSocketMessageHandler getHandler() {
        return handler;
    }

    public ChannelHandlerContext getContext() {
        return ctx;
    }

    public ChannelFuture write(String msg) {
        return ctx.write(new TextWebSocketFrame(msg));
    }

    public ChannelFuture write(WebSocketFrame frame) {
        return ctx.write(frame);
    }

    public ChannelFuture writeAndFlush(String msg) {
        return ctx.writeAndFlush(new TextWebSocketFrame(msg));
    }

    public ChannelFuture writeAndFlush(WebSocketFrame frame) {
        return ctx.writeAndFlush(frame);
    }

    public void flush() {
        ctx.channel().flush();
    }

    public ChannelFuture close() {
        return ctx.channel().close();
    }

}
