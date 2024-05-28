package or.lotus.common.http.simpleserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import or.lotus.common.http.WebSocketFrame;
import or.lotus.common.http.simpleserver.support.HttpFormData;
import or.lotus.common.http.simpleserver.support.HttpProtocolCodec;
import or.lotus.common.http.simpleserver.support.HttpRequest;
import or.lotus.common.http.simpleserver.support.HttpResponse;
import or.lotus.common.http.simpleserver.support.HttpServerX509TrustManager;
import or.lotus.common.http.simpleserver.support.HttpsProtocolCodec;
import or.lotus.common.http.simpleserver.support.ResponseStatus;
import or.lotus.common.http.simpleserver.support.SSLClosedException;
import or.lotus.common.http.simpleserver.support.SSLState;
import or.lotus.common.http.simpleserver.support.WebSocketProtocolCodec;
import or.lotus.common.nio.IoHandler;
import or.lotus.common.nio.Session;
import or.lotus.common.nio.tcp.NioTcpServer;
import or.lotus.common.nio.tcp.NioTcpSession;

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
    private int                         requestMaxLimit     =   1024 * 1024 * 4;//4M
    private boolean                     enableSSL           =   false;
    private SSLContext                  sslContext          =   null;
    private HttpProtocolCodec           httpProtocolCodec   =   null;
    private HttpsProtocolCodec          httpsProtocolCodec  =   null;
    private boolean                     isNeedClientAuth    =   false;
    private int                         oldBufferSize       =   0;

    public HttpServer() {
        server = new NioTcpServer();

        server.setHandler(ioHandler);
        uploadTmpDir = System.getProperty("java.io.tmpdir") + File.separator;
        charset = Charset.forName("utf-8");
        httpProtocolCodec = new HttpProtocolCodec(this);
        server.setProtocolCodec(httpProtocolCodec);
        //连接空闲时关闭
        setTimeOut(1000 * 20);
    }

    public void setKeyStoreAndEnableSSL(String keystore, String password) throws Exception {
        setKeyStoreAndEnableSSL(keystore, password, "TLS");
    }

    /**
     * java1.8最高只支持tls1.2
     * 如果要jdk1.8支持 tls1.3 需要加另外的库
     * https://github.com/openjsse/openjsse
     * https://blog.csdn.net/devzyh/article/details/122074632
     * 启用ssl后不要当静态文件服务器
     * @param keystore
     * @param password
     * @param protocol jdk8(SSL,SSLv2,SSLv3,TLS,TLSv1,TLSv1.1,TLSv1.2) the standard name of the requested protocol.
     *          See the SSLContext section in the <a href=
     * "{@docRoot}/../technotes/guides/security/StandardNames.html#SSLContext">
     *          Java Cryptography Architecture Standard Algorithm Name
     *          Documentation</a>
     *          for information about standard protocol names.
     * @throws Exception
     */
    public void setKeyStoreAndEnableSSL(String keystore, String password, String protocol) throws Exception {
        setKeyStoreAndEnableSSL(keystore, password, protocol, isNeedClientAuth, 10 * 1000);
    }

    public void setKeyStoreAndEnableSSL(String keystore, String password, String protocol, boolean needClientAuth, int handshakeTimeOut) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {

        File fKeyStore = new File(keystore);
        if(!fKeyStore.exists()) {
            throw new FileNotFoundException("keystore file not found");
        }
        char[] passphrase = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(fKeyStore), passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        sslContext = SSLContext.getInstance(protocol);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        sslContext.init(
                kmf.getKeyManagers(),
                new TrustManager[] { new HttpServerX509TrustManager(this) },
                null);
        httpsProtocolCodec = new HttpsProtocolCodec(this);
        server.setProtocolCodec(httpsProtocolCodec);
        isNeedClientAuth = needClientAuth;
        enableSSL = true;
    }

    public boolean isNeedClientAuth() {
        return isNeedClientAuth;
    }

    public HttpProtocolCodec getHttpProtocolCodec() {
        return httpProtocolCodec;
    }

    public boolean isTcpNoDelay() {
        return server.isTcpNoDelay();
    }

    public void setTcpNoDelay(boolean noDelay) {
        server.setTcpNoDelay(noDelay);
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    public boolean isEnableSSL() {
        return enableSSL;
    }

    public int getRequestMaxLimit() {
        return requestMaxLimit;
    }

    public void setRequestMaxLimit(int limit) {
        requestMaxLimit = limit;
    }

    public String getUploadTempDir() {
        return uploadTmpDir;
    }

    public synchronized void reSizeCacheBuffer(int size) {
        if(size != oldBufferSize) {
            //System.out.println("resize:" + size);
            server.setSessionCacheBufferSize(Math.max(oldBufferSize, size));
            oldBufferSize = size;
        }
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

    /**
     * 启用https时, 系统会自动覆盖此值, 以避免此值过小导致频繁分配内存
     * @param bufferCacheSize
     */
    public void setCacheBufferSize(int bufferCacheSize) {
        oldBufferSize = bufferCacheSize;
        server.setSessionCacheBufferSize(bufferCacheSize);
    }

    public boolean isOpenWebSocket() {
        return enableWebSocket;
    }

    /**
     * https://tools.ietf.org/html/rfc6455#page-31
     * @param enable
     */
    public void enableWebSocket(boolean enable) {
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
     * @param handler
     */
    public synchronized void setHandler(HttpHandler handler){
        this.handler = handler;
    }

    public void start(InetSocketAddress addr) throws IOException {
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

    private void freeSSLEngine(Session session) {
        SSLState ssl = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
        if(ssl != null) {
            ssl.close();
            session.removeAttr(SSLState.SSL_STATE_KEY);
        }
    }

    private IoHandler wsIoHandler = new  IoHandler() {

        public void onClose(Session session) throws Exception {
            freeSSLEngine(session);
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
            if(frame.opcode == WebSocketFrame.OPCODE_CLOSE) {
                session.write(WebSocketFrame.close());
                session.closeOnFlush();
                return;
            }
            handler.wsMessage(session, request, frame);
        };

        public void onIdle(Session session) throws Exception {
            HttpRequest request = (HttpRequest) session.getAttr(WS_HTTP_REQ);
            handler.wsIdle(session, request);
        };


        @Override
        public void onException(Session session, Throwable e) {
            try {
                HttpRequest request = (HttpRequest) session.getAttr(WS_HTTP_REQ);

                handler.exception(e, request, null);

                //session.closeNow();

            } catch(Exception e2) {
                e2.printStackTrace();
            }
        }
    };

    private IoHandler ioHandler = new IoHandler() {

        public boolean onBeforeConnection(Session session) throws Exception {
            if(enableSSL) {
                NioTcpSession tcpSession = (NioTcpSession) session;
                //处理https握手
                try {
                    //如果返回了buf则表示https握手数据有剩余的此时会在IoProcess执行和读事件一样的代码
                    ByteBuffer buf = httpsProtocolCodec.doHandshake(tcpSession);
                    if(buf != null) {
                        session.updateReadCacheBuffer(buf);
                    }
                } catch(Exception e) {
                    freeSSLEngine(session);
                    session.closeNow();
                    //System.out.println("exception:" + session.getId() + " msg:" + e.getMessage());
                    return false;
                }

            }
            return true;
        };

        public void onConnection(Session session) throws Exception {
            /*DebugLogFileManager.getInstance().append(
                    String.format("session-%d", session.getId())
                    , "connection:" + session.toString());*/
        };

        @Override
        public void onRecvMessage(Session session, Object msg)throws Exception {

            if(msg == null) {
                return;
            }
            HttpRequest request = null;
            HttpResponse response = null;
            try{
                request = (HttpRequest) msg;

                response = HttpResponse.defaultResponse(session, request);
                response.setCharacterEncoding(request.getCharacterEncoding());

                if(request.isWebSocketConnection()) {
                    session.setProtocolCodec(new WebSocketProtocolCodec(HttpServer.this));
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

                /*DebugLogFileManager.getInstance().append(
                        String.format("session-%d", session.getId())
                        , "recv http request:" + request.getPath());*/
                if(handler != null) {
                    handler.service(request.getMethod(), request, response);
                    response.flush();
                    if("close".equals(request.getHeader("connection"))) {
                        /*
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
            }catch(Throwable e) {
                response.setStatus(ResponseStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
                handler.exception(e, request, response);
            }
            response.free();
            try {
                if(request != null && request.isFormData()) {
                    HttpFormData formData = request.getFormData();

                    formData.removeCache();
                    formData.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        public void onClose(Session session) throws Exception {
            freeSSLEngine(session);
            /*DebugLogFileManager.getInstance().append(
                    String.format("session-%d", session.getId())
                    , "close:" + session.toString());*/
        };

        public void onSentMessage(Session session, Object msg) throws Exception {

        };

        @Override
        public void onIdle(Session session) throws Exception {

            session.closeNow();
        }

        @Override
        public void onException(Session session, Throwable e) {
            try {
                if(e instanceof SSLClosedException) {

                } else {
                    handler.exception(e, null, null);
                }
                session.closeNow();

            } catch(Exception e2) {
                e2.printStackTrace();
            }
        }

    };
}
