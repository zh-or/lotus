package lotus.http.server;

import java.nio.ByteBuffer;
import java.util.HashMap;

import lotus.nio.Session;


public class Response {
    private ByteBuffer              buffer;
    private Session                 session;
    private Request                 request;
    private HashMap<String, String> headers;
    
    public Response(Session session, Request request) {
        this.session = session;
        this.request = request;
        this.headers = new HashMap<String, String>();
    }
    
    /**
     * 发送302跳转
     * @param path
     */
    public void sendRedirect(String path){
        
    }
    
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }
    
    public void send(){
        session.write(this);
    }
    
    public void close(){
        session.closeOnFlush();
    }
    
    
}
