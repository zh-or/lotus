package lotus.http.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import lotus.http.WebSocketFrame;
import lotus.http.server.support.HttpProtocolCodec;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.ResponseStatus;
import lotus.http.server.support.WebSocketProtocolCodec;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpServer;

/*
 * 一个简单的http服务器
 * */
public class HttpServer {
    private static final String          WS_HTTP_REQ        =   "_____________WS_HTTP_REQ_______________";
    
    private NioTcpServer                server              =   null;
    private Charset                     charset             =   null;
    private boolean                     enableWebSocket     =   false;
    private HttpHandler                 handler             =   null;
    private String                      uploadTmpDir        =   null;
    
    public HttpServer() {
        server = new NioTcpServer();
        server.setHandler(ioHandler);
        uploadTmpDir = System.getProperty("java.io.tmpdir") + File.separator;
        charset = Charset.forName("utf-8");
        server.setProtocolCodec(new HttpProtocolCodec(this));
        server.setSessionIdleTime(20000);/*keep-alive*/
    }
    
    public String getUploadTempDir() {
        return uploadTmpDir;
    }
    
    /**
     * 设置上传文件保存的临时目录
     * @param dir
     */
    public void setUploadTempDir(String dir) {
        uploadTmpDir = dir;
    }
    
    public void setEventThreadPoolSize(int size) {
        server.setEventThreadPoolSize(size);
    }
    
    public void setReadBufferCacheSize(int ReadBufferCacheSize){
        server.setSessionCacheBufferSize(ReadBufferCacheSize);
    }

    public boolean isOpenWebSocket(){
        return enableWebSocket;
    }
    
    /**
     * https://tools.ietf.org/html/rfc6455#page-31
     * @param open
     */
    public void enableWebSocket(boolean enable){
        this.enableWebSocket = enable;
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
    public synchronized void setHandler(HttpHandler handler){
        this.handler = handler;
    }

    public void start(InetSocketAddress addr) throws IOException{
        server.start(addr);
    }
    
    public void stop() {
        if(server != null) {
            server.close();
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


    private IoHandler wsIoHandler = new  IoHandler() {

        public void onClose(Session session) throws Exception {
            if(handler == null) {
                return;
            }
            HttpRequest request  = (HttpRequest) session.getAttr(WS_HTTP_REQ);

            handler.wsClose(session, request);
        };

        public void onRecvMessage(Session session, Object msg) throws Exception {
            if(handler == null) {
                return;
            }
            WebSocketFrame frame    = (WebSocketFrame) msg;
            HttpRequest    request  = (HttpRequest) session.getAttr(WS_HTTP_REQ);

            handler.wsMessage(session, request, frame);
        };

        public void onIdle(Session session) throws Exception {
            /*这里可以做成多久没有操作 就关闭该 ws 通道*/
        };
    };

    private IoHandler ioHandler = new IoHandler() {
        @Override
        public void onRecvMessage(Session session, Object msg)throws Exception {
            HttpRequest request = null;
            HttpResponse response = null;
            try{
                request = (HttpRequest) msg;

                response = HttpResponse.defaultResponse(session, request);
                response.setCharacterEncoding(request.getCharacterEncoding());

                if(request.isWebSocketConnection()) {
                    session.setProtocolCodec(new WebSocketProtocolCodec());
                    session.setIoHandler(wsIoHandler);
                    session.setAttr(WS_HTTP_REQ, request);
                
                    response.flush();
                    if(handler == null) {
                        return;
                    }

                    handler.wsConnection(session, request);
                    return;
                }

                response.setHeader("Content-Type", "text/html; charset=" + charset.displayName());

                if(handler != null) {
                    handler.service(request.getMothed(), request, response);
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
                } else {
                    response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
                    response.flush();
                    session.closeOnFlush();
                }
            }catch(Throwable e){
                handler.exception(e, request, response);
            }
            
        }
        
        public void onSentMessage(Session session, Object msg) throws Exception {
            
        };

        @Override
        public void onIdle(Session session) throws Exception {

            session.closeNow();
        }

        @Override
        public void onException(Session session, Throwable e) {
            try {
                session.closeNow();
                handler.exception(e, null, null);
            } catch(Exception e2) {
                e2.printStackTrace();
            }
        }

    };
}
