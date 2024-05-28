package or.lotus.common.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;

public abstract class WebSocketMessageHandler {
    private String path;

    public WebSocketMessageHandler(String path) {
        this.path = path;
    }

    public void onConnection(WebSocketSession session) throws Exception {

    }

    public void onMessage(WebSocketSession session, String msg) throws Exception {

    }

    public void onClose(WebSocketSession session) throws Exception {
        session.close();
    }

    public void onBinaryMessage(WebSocketSession session, BinaryWebSocketFrame frame) {
    }

    /**
     * 此事件应该不会被触发
     * @param session
     * @param frame
     */
    public void onPing(WebSocketSession session, PingWebSocketFrame frame) {
        session.write(new PongWebSocketFrame(frame.content().retain()));
    }

    public void onContinuationMessage(WebSocketSession session, ContinuationWebSocketFrame frame) {
    }

    public void onException(ChannelHandlerContext ctx, Exception e) {
        e.printStackTrace();
    }

    public String getPath() {
        return path;
    }
}
