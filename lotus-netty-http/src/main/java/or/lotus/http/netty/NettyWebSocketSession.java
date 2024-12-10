package or.lotus.http.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.net.SocketAddress;
import java.util.HashMap;

public class NettyWebSocketSession {
    private QueryStringDecoder qsd;
    private String path;
    private NettyWebSocketMessageHandler handler;
    private ChannelHandlerContext ctx;
    private HttpHeaders requestHeaders;
    protected HashMap<String, Object> attributes;

    public NettyWebSocketSession(QueryStringDecoder qsd, HttpHeaders headers, NettyWebSocketMessageHandler handler, ChannelHandlerContext ctx) {
        this.qsd = qsd;
        this.requestHeaders = headers;
        this.path = qsd.path();
        this.handler = handler;
        this.ctx = ctx;
        this.attributes = new HashMap<>();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
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

    public NettyWebSocketMessageHandler getHandler() {
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

    public SocketAddress getRemoteAddress() {
        return ctx.channel().remoteAddress();
    }

}
