package lotus.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;

import lotus.http.server.support.Filter;
import lotus.http.server.support.HttpHandler;
import lotus.http.server.support.HttpProtocolCodec;
import lotus.http.server.support.WebSocketHandler;
import lotus.http.server.support.WsProtocolCodec;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpServer;
import lotus.utils.Utils;

/*
 * 一个简单的http服务器
 * */
public class HttpServer {

    public static final byte            OPCODE_TEXT         =   1;
    public static final byte            OPCODE_BINARY       =   2;
    public static final byte            OPCODE_CLOSE        =   8;
    public static final byte            OPCODE_PING         =   9;
    public static final byte            OPCODE_PONG         =   10;
    
    public static final int             SERVER_TYPE_HTTP    =   1;
    public static final int             SERVER_TYPE_HTTPS   =   2;
    
    
    public static final String          WS_BASE_PATH        =   "_____________WS_BASE_PATH_______________";
    public static final String          WS_QUERY_STR        =   "_____________WS_QUERY_STR_______________";
    
    
    

//  private Log         log;
    private ArrayList<Filter> filters               =   null;
    private NioTcpServer      server                =   null;
    private Charset           charset               =   null;
    private boolean           openWebSocket         =   false;
    private String            file_keystore         =   null;
    private WebSocketHandler  wsHandler             =   null;
    
    public HttpServer() throws IOException{
        filters = new ArrayList<Filter>();
        server = new NioTcpServer();
        server.setHandler(new HttpEventHandler());
        /*
        log = Log.getInstance();
        log.setProjectName("simpli http server");*/
        charset = Charset.forName("utf-8");
        server.setProtocolCodec(new HttpProtocolCodec(this));
        server.setSessionIdleTime(20000);/*keep-alive*/
        wsHandler = new WebSocketHandler() {
        };
    }
    
    public void setEventThreadPoolSize(int size) {
        server.setEventThreadPoolSize(size);
    }
    
    public void setReadBufferCacheSize(int ReadBufferCacheSize){
        server.setSessionCacheBufferSize(ReadBufferCacheSize);
    }
    
    
    public void setWebSocketHandler(WebSocketHandler wsHandler){
        this.wsHandler = wsHandler;
    }
    
    public boolean isOpenWebSocket(){
        return openWebSocket;
    }
    
    public void openWebSocket(boolean open){
        this.openWebSocket = open;
    }
    
    
    public void setKeystoreFilePath(String path) {
        this.file_keystore = path;
    }
    
    /**
     * 设置连接超时时间, 超时后会关闭该连接
     * @param t 毫秒
     */
    public void setTimeOut(int t){
        server.setSessionIdleTime(t);/*keep-alive*/
    }
    
    /**
     * 先添加先调用, 顺序查找handler
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
        
        server.start(addr);
    }
    
    public void stop(){
        if(server != null) {
            server.close();
        }
    }
    
    public IoHandler getWsEventHandler(){
        return WebSocketEventHandler;
    }
    
    private IoHandler WebSocketEventHandler = new  IoHandler() {
        
        public void onClose(Session session) throws Exception {
            wsHandler.WebSocketClose(session);
        };
        
        public void onRecvMessage(Session session, Object msg) throws Exception {
            WsRequest request = (WsRequest) msg;
            request.basePath = (String) session.getAttr(WS_BASE_PATH);
            request.queryString = (String) session.getAttr(WS_QUERY_STR);
            if(request.op == OPCODE_PING){
                wsHandler.WebSocketPing(session);
            }else if(request.op == OPCODE_CLOSE){
                session.write(WsResponse.close());
            }else{
                wsHandler.WebSocketMessage(session, request);
            }
            
        };
        
        public void onIdle(Session session) throws Exception {
            /*这里可以做成多久没有操作 就关闭该 ws 通道*/
        };
    };
    
    private class HttpEventHandler extends lotus.nio.IoHandler{
        
        @Override
        public void onRecvMessage(Session session, Object msg)throws Exception {
            HttpRequest request = (HttpRequest) msg;
            
            HttpResponse response = HttpResponse.defaultResponse(session, request);
            response.setCharacterEncoding(request.getCharacterEncoding());
            
            if(request.isWebSocketConnection()){
                session.setProtocolCodec(new WsProtocolCodec());
                session.setIoHandler(WebSocketEventHandler);
                if(request.getPath() != null){
                    session.setAttr(WS_BASE_PATH, request.getPath());
                }
                if(request.getQueryString() != null) {
                    session.setAttr(WS_QUERY_STR, request.getQueryString());
                }
                response.flush();
                wsHandler.WebSocketConnection(session);
                return;
            }
            
            response.setHeader("Content-Type", "text/html; charset=" + charset.displayName());
            String url = request.getPath(), url_end;
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
                        if("close".equals(request.getHeader("connection"))){
                            /*简单判断
                             * keep-alive 则不关闭
                             * */
                            session.closeOnFlush();
                        }
                        if(response.isOpenSync()) {
                            response.syncEnd();
                        }
                        return;
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
            
            response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
            response.flush();
            session.closeOnFlush();
            
        }
        
        @Override
        public void onException(Session session, Throwable e) {
            session.closeNow();
        	e.printStackTrace();
        }
        
        @Override
        public void onConnection(Session session) {
//            log.info("connection:" + session.getRemoteAddress());
        }
        
        @Override
        public void onClose(Session session) {

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
