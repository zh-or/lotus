package or.lotus.core.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import or.lotus.core.http.server.WebSocketSession;

public abstract class NettyWebSocketMessageHandler {
    private String path;

    public NettyWebSocketMessageHandler(String path) {
        this.path = path;
    }

    public void onConnection(NettyWebSocketSession session) throws Exception {

    }

    public void onMessage(NettyWebSocketSession session, String msg) throws Exception {

    }

    public void onClose(NettyWebSocketSession session) throws Exception {
        session.close();
    }

    public void onBinaryMessage(NettyWebSocketSession session, BinaryWebSocketFrame frame) {
    }

    /**
     * 此事件应该不会被触发
     * @param session
     * @param frame
     */
    public void onPing(NettyWebSocketSession session, PingWebSocketFrame frame) {
        session.write(new PongWebSocketFrame(frame.content().retain()));
    }

    public void onContinuationMessage(NettyWebSocketSession session, ContinuationWebSocketFrame frame) {
    }

    public void onException(ChannelHandlerContext ctx, Exception e) {
        e.printStackTrace();
    }

    public String getPath() {
        return path;
    }
}
