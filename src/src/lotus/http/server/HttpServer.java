package lotus.http.server;

import java.net.InetSocketAddress;

import lotus.log.Log;
import lotus.nio.NioContext;
import lotus.nio.Session;
import lotus.nio.tcp.TcpServer;

/*
 * 一个简单的http服务器
 * */
public class HttpServer {

    private HttpHandler handler;
    private NioContext  server;
    private Log         log;
    
    public HttpServer(){
        this.handler = new HttpHandler() {};
        server = new TcpServer(0, 100, 1024);
        server.setProtocolCodec(new HttpProtocolCodec());
        server.setSessionIdleTime(20000);
        server.setSessionReadBufferSize(1024);
        server.setHandler(new EventHandler());
        log = Log.getInstance();
        log.setProjectName("simpli http server");
    }
    
    public void setHandler(HttpHandler handler){
        this.handler = handler;
    }
    
    public boolean start(InetSocketAddress addr){
        try {
            server.bind(addr);
            return true;
        } catch (Exception e) {}
        return false;
    }
    
    public void stop(){
        server.unbind();
    }
    
    private class EventHandler extends lotus.nio.IoHandler{
        
        @Override
        public void onRecvMessage(Session session, Object msg) {
            Request request = (Request) msg;
            handler.service(request.getMothed(), request, new Response( ));
        }
        
        @Override
        public void onException(Session session, Exception e) {
            e.printStackTrace();
        }
        
        @Override
        public void onConnection(Session session) {
            log.info("connection:" + session.getRemoteAddress());
        }
        
        @Override
        public void onIdle(Session session) {
            session.closeNow();
            log.warn("idle, close:" + session.getRemoteAddress());
        }
        
        @Override
        public void onClose(Session session) {
            log.info("close:" + session.getRemoteAddress());
        }
    }
}
