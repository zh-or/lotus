package lotus.http.server;

import lotus.nio.Session;

public abstract class WebSocketHandler {
    
    public void WebSocketConnection(Session session){
        
    }
    
    public void WebSocketMessage(Session session, WsRequest request){
        
    }
    
    public void WebSocketClose(Session session){
        
    }
    
    public void WebSocketPing(Session session){
        session.write(WsResponse.pong());
    }
}