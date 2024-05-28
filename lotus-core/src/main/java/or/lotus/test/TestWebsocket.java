package or.lotus.test;

import or.lotus.http.server.HttpServer;
import or.lotus.http.server.WebSocketMessageHandler;
import or.lotus.http.server.WebSocketSession;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;

import java.net.InetSocketAddress;

public class TestWebsocket extends WebSocketMessageHandler {
    public TestWebsocket(String path) {
        super(path);
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer();
        server.addWebSocketHandler(new TestWebsocket("/test"));
        server.start();
        server.bind(new InetSocketAddress(8080));
        System.out.println("websocket 服务器启动完成: 8080");
    }

    @Override
    public void onConnection(WebSocketSession session) throws Exception {
        System.out.println(session.hashCode() + " 连接");
    }

    @Override
    public void onClose(WebSocketSession session) throws Exception {
        System.out.println(session.hashCode() + " 断开");
    }

    @Override
    public void onMessage(WebSocketSession session, String msg) throws Exception {
        System.out.println(session.hashCode() + " 收到文本消息:" + msg);
        session.write("fuck u");
    }

    @Override
    public void onPing(WebSocketSession session, PingWebSocketFrame frame) {
        System.out.println(session.hashCode() + " ping");

    }
}
