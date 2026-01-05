package or.lotus.core.nio.http;

import or.lotus.core.http.WebSocketFrame;
import or.lotus.core.nio.Session;

public abstract class HttpWebSocketMessageHandler {
    private String path;

    public HttpWebSocketMessageHandler(String path) {
        this.path = path;
    }

    public void onConnection(HttpRequest request, Session session) throws Exception {

    }

    public void onMessage(Session session, WebSocketFrame msg) throws Exception {

    }

    public void onClose(Session session) throws Exception {

    }

    public void onIdle(Session session) throws Exception {
    }

    public void onException(Session session, Throwable e) {
        e.printStackTrace();
    }

    public String getPath() {
        return path;
    }
}
