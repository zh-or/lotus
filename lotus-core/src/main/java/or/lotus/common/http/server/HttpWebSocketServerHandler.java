package or.lotus.common.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;

public class HttpWebSocketServerHandler  extends SimpleChannelInboundHandler<WebSocketFrame> {
    final static AttributeKey<WebSocketSession> SESSION_ATTRIBUTE_KEY = AttributeKey.valueOf(WebSocketSession.class, "WebSocketSession");
    final static AttributeKey<WebSocketServerProtocolHandler.HandshakeComplete> HANDSHAKER_ATTR_KEY = AttributeKey.valueOf(WebSocketServerProtocolHandler.HandshakeComplete.class, "HANDSHAKER");
    HttpServer server;
    public HttpWebSocketServerHandler(HttpServer server) {
        this.server = server;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete info = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            QueryStringDecoder qsd = new QueryStringDecoder(info.requestUri());

            WebSocketMessageHandler handler = server.getWebSocketMessageHandler(qsd.path());
            WebSocketSession session = new WebSocketSession(qsd, info.requestHeaders(), handler, ctx);

            ctx.attr(SESSION_ATTRIBUTE_KEY).set(session);
            server.exec(() -> {
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
        WebSocketSession session = ctx.attr(SESSION_ATTRIBUTE_KEY).get();
        if(session != null) {
            server.exec(() -> {
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

        WebSocketSession session = ctx.attr(SESSION_ATTRIBUTE_KEY).get();
        if(session == null) {
            throw new Exception("错误的连接到了错误的handler:" + frame.toString());
        }
        WebSocketFrame newFrame = server.eventExec != null ? frame.retain() : frame;

        server.exec(() -> {
            WebSocketMessageHandler handler = session.getHandler();
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
                if(server.eventExec!= null) {
                    //手动调用会报错
                    frame.release();
                }
            }
        });
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


}
