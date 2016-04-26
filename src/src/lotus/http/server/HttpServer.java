package lotus.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

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
    private Charset     charset;
    
    
    public HttpServer(){
        this.handler = new HttpHandler() {};
        server = new TcpServer(0, 100, 1024);
        server.setSessionIdleTime(20000);
        server.setSessionReadBufferSize(1024);
        server.setHandler(new EventHandler());
        log = Log.getInstance();
        log.setProjectName("simpli http server");
        this.charset = Charset.forName("utf-8");
        server.setProtocolCodec(new HttpProtocolCodec(this));
    }
    
    public void setHandler(HttpHandler handler){
        this.handler = handler;
    }
    
    public void start(InetSocketAddress addr) throws IOException{
        server.bind(addr);
    }
    
    public void stop(){
        server.unbind();
    }
    
    private class EventHandler extends lotus.nio.IoHandler{
        
        @Override
        public void onRecvMessage(Session session, Object msg) {
            Request request = (Request) msg;
            handler.service(request.getMothed(), request, new Response(session, request));
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
            
            log.warn("idle:" + session.getRemoteAddress());
        }
        
        @Override
        public void onClose(Session session) {
            log.info("close:" + session.getRemoteAddress());
        }
    }

    public void setCharset(Charset charset){
        this.charset = charset;
    }
    
    public Charset getCharset() {
        return this.charset;
    }
}
