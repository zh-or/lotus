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
    
    public HttpServer(int selectorThreadTotal, int eventThreadTotal, int readBufferSize){
        this.handler = new HttpHandler() {};
        server = new TcpServer(selectorThreadTotal, eventThreadTotal, 1024);
        server.setSessionIdleTime(0);
        server.setSessionReadBufferSize(readBufferSize);
        server.setHandler(new EventHandler());
        
        log = Log.getInstance();
        log.setProjectName("simpli http server");
        this.charset = Charset.forName("utf-8");
        server.setProtocolCodec(new HttpProtocolCodec(this));
        
        server.setSessionIdleTime(20000);/*keep-alive*/
    }
    
    /**
     * 设置连接超时时间, 超时后会关闭该连接
     * @param t 毫秒
     */
    public void setTimeOut(int t){
        server.setSessionIdleTime(t);/*keep-alive*/
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
        public void onRecvMessage(Session session, Object msg)throws Exception {
            HttpRequest request = (HttpRequest) msg;
            HttpResponse response = HttpResponse.defaultResponse(session, request);
            handler.service(request.getMothed(), request, response);
            response.flush();
            if("close".equals(request.getHeader("connection"))){/*简单判断*/
            	session.closeOnFlush();
            }
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
        public void onClose(Session session) {
            log.info("close:" + session.getRemoteAddress());
        }
        
        @Override
        public void onIdle(Session session) throws Exception {
            session.closeNow();
        }
    }

    /**
     * 设置全局编码方式
     * @param charset
     */
    public void setCharset(Charset charset){
        this.charset = charset;
    }
    
    public Charset getCharset() {
        return this.charset;
    }
}
