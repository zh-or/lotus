package lotus.http.server.support;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lotus.http.server.WsRequest;
import lotus.http.server.WsResponse;
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
    
    
    public String getParameter(String name, String path) {
        try {
            Matcher m = Pattern.compile("[&?]" + name + "=([^&]*)").matcher("&" + path);
            if(m.find()){
                return URLDecoder.decode(m.group(1), "utf-8");
            }
        } catch (Exception e) { }
        return null;
    }
}
