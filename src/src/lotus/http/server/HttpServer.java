package lotus.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;

import lotus.nio.Session;
import lotus.nio.tcp.NioTcpServer;
import lotus.utils.Utils;

/*
 * 一个简单的http服务器
 * */
public class HttpServer {

//  private Log         log;
    private ArrayList<Filter> filters;
    private NioTcpServer  server;
    private Charset     charset;
    
    public HttpServer(int EventThreadTotal) throws IOException{
        filters = new ArrayList<Filter>();
        server = new NioTcpServer();
        server.setEventThreadPoolSize(EventThreadTotal);
        server.setHandler(new EventHandler());
        /*
        log = Log.getInstance();
        log.setProjectName("simpli http server");*/
        this.charset = Charset.forName("utf-8");
        server.setProtocolCodec(new HttpProtocolCodec(this));
        
        server.setSessionIdleTime(20000);/*keep-alive*/
    }
    
    public void setReadBufferCacheSize(int ReadBufferCacheSize){
        server.setSessionCacheBufferSize(ReadBufferCacheSize);
    }
    
    /**
     * 设置连接超时时间, 超时后会关闭该连接
     * @param t 毫秒
     */
    public void setTimeOut(int t){
        server.setSessionIdleTime(t);/*keep-alive*/
    }
    
    /**
     * 
     * @param path  <br> 
     *   三种类型  :<br>
     *      1. * 表示所有请求都监听<br>
     *      2. *.xxx xxx表示后缀<br>
     *      3. path 完全的路径, 此方式则需要在最前面添加 '/'<br>
     * @param handler
     */
    public synchronized void addHandler(String path, HttpHandler handler){
        filters.add(new Filter(path, handler));
    }
    
    public synchronized void removeHandler(String path){
        if(Utils.CheckNull(path)) return ;
        Filter filter;
        for(int i = filters.size() - 1; i >= 0; i --){
            filter = filters.get(i);
            if(path.equals(filter.path)){
                filters.remove(i);
            }
        }
    }
    
    public void start(InetSocketAddress addr) throws IOException{
        server.bind(addr);
    }
    
    public void stop(){
        server.close();
    }
    
    private class EventHandler extends lotus.nio.IoHandler{
        
        @Override
        public void onRecvMessage(Session session, Object msg)throws Exception {
            HttpRequest request = (HttpRequest) msg;
            HttpResponse response = HttpResponse.defaultResponse(session, request);
            response.setCharacterEncoding(request.getCharacterEncoding());
            response.setHeader("Content-Type", "text/html; charset=" + charset.displayName());
            String url = request.getPath(), url_end;
            boolean dohandler = false;
            int len = url.length();
            if(len > 0){
                int p = url.lastIndexOf(".");
                url_end = p != -1 ? url.substring(p, len) : url;
            }else{
                url_end = "";
            }
            for(Filter filter : filters) {
                if("*".equals(filter.path) || (filter.path.startsWith("*") && filter.path.endsWith(url_end)) || url.equals(filter.path)){
                    if(filter != null){
                        filter.handler.service(request.getMothed(), request, response);
                        response.flush();
                        if("close".equals(request.getHeader("connection"))){/*简单判断*/
                            session.closeOnFlush();
                        }
                        dohandler = true;
                        break;
                    }
                }
            }
            
            
            /*Filter filter = null;
            for(int i = filters.size() - 1; i >= 0; i --){
                filter = filters.get(i);
                //System.out.println(String.format("filter:%s, url_end:%s", filter.path, url_end));
                if("*".equals(filter.path) || (filter.path.startsWith("*") && filter.path.endsWith(url_end)) || url.equals(filter.path)){
                    if(filter != null){
                        filter.handler.service(request.getMothed(), request, response);
                        response.flush();
                        if("close".equals(request.getHeader("connection"))){简单判断
                            session.closeOnFlush();
                        }
                        dohandler = true;
                        break;
                    }
                }
            }*/
            if(dohandler) return;
            response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
            response.flush();
            session.closeOnFlush();
            
        }
        
        @Override
        public void onException(Session session, Exception e) {
            
        	e.printStackTrace();
        }
        
        @Override
        public void onConnection(Session session) {
//            log.info("connection:" + session.getRemoteAddress());
        }
        
        @Override
        public void onClose(Session session) {
//            log.info("close:" + session.getRemoteAddress());
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
