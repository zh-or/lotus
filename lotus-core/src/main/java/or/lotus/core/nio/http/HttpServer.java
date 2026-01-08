package or.lotus.core.nio.http;

import or.lotus.core.common.Utils;
import or.lotus.core.http.HttpFileFilter;
import or.lotus.core.http.WebSocketFrame;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.support.RestfulResponseStatus;
import or.lotus.core.http.restful.support.RestfulUtils;
import or.lotus.core.nio.IoHandler;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.tcp.NioTcpServer;
import or.lotus.core.nio.tcp.NioTcpSession;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 默认配置:
 * 1. 当post的内容超过4M时使用文件缓存
 * 2. ByteBuffer单个缓存大小为 16kb
 * 3. UploadTmpDir 默认为系统临时文件目录
 * 4. 默认没有静态文件目录, 如果需要支持静态文件访问请调用 addStaticPath 添加目录
 * 5. 当使用 / 根路径访问时, 如果RESTFul为处理则默认为访问index.html文件
 *
 * */
public class HttpServer extends RestfulContext {
    protected NioTcpServer server;
    protected String uploadTmpDir = null;
    protected HttpFileFilter fileFilter;
    protected List<String> staticPath = new ArrayList<>(3);
    protected boolean isSupportSymbolicLink = false;
    protected String defaultIndexFile = "index.html";
    protected boolean enableSSL = false;
    protected SSLContext sslContext = null;
    /** 如果http内容超过此值, 则缓存到文件 */
    protected int cacheContentToFileLimit = 1024 * 1024 * 4;

    /** 每个连接最大请求数量 */
    protected int keepLiveRequestCount = 10;
    protected int bufferCapacity = 1024 * 16;

    protected HashMap<String, HttpWebSocketMessageHandler> webSocketHandlers = new HashMap<>();

    protected WebSocketProtocolCodec webSocketProtocolCodec = null;

    public HttpServer() {
        server = new NioTcpServer();
        server.setHandler(new HttpIoHandler());
        server.setProtocolCodec(new HttpProtocolCodec(this));
        //调整为使用NioTcpServer的线程池, 方便事件内部同步处理资源释放
        eventThreadPoolSize = 0;
    }

    @Override
    public void setEventThreadPoolSize(int eventThreadPoolSize) {
        //调整为使用NioTcpServer的线程池, 方便事件内部同步处理资源释放
        if(eventThreadPoolSize > 0) {
            server.setExecutor(Executors.newFixedThreadPool(
                    eventThreadPoolSize,
                    (run) -> new Thread(run, "lotus-http-service-pool")
            ));
        }
    }

    @Override
    protected void onStart() throws Exception {
        if(fileFilter != null) {
            try {
                RestfulUtils.injectBeansToObject(this, fileFilter);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if(isEnableWebSocket()) {
            webSocketProtocolCodec = new WebSocketProtocolCodec(HttpServer.this);
        }

        server.setBufferCapacity(bufferCapacity);
        server.setTcpNoDelay(true);
        server.setExecutor(executorService);
        //server.setMaxMessageSendListCapacity(1024);
        server.setSessionIdleTime(60 * 1000);//空闲时关闭该链接
        server.bind(bindAddress);
        server.start();
    }

    @Override
    protected void onStop() {
        server.stop();
    }

    /***
     * @param keystore 路径
     * @param password 密码
     * @param keyStoreType 私钥类型 JKS 或 PKCS12）, 推荐 PKCS12
     * @param protocol  jdk8(SSL,SSLv2,SSLv3,TLS,TLSv1,TLSv1.1,TLSv1.2) the standard name of the requested protocol.
     *          See the SSLContext section in the <a href=
     * "{@docRoot}/../technotes/guides/security/StandardNames.html#SSLContext">
     *          Java Cryptography Architecture Standard Algorithm Name
     *          Documentation</a>
     *          for information about standard protocol names.
     */
    public void setKeyStoreAndEnableSSL(String keystore, String password, String keyStoreType, String protocol) throws Exception {
        char[] pwdCharArr = password.toCharArray();
        KeyStore ks = KeyStore.getInstance(keyStoreType); // 加载服务端证书密钥库（JKS 或 PKCS12）, 推荐 PKCS12
        try (FileInputStream fis = new FileInputStream(keystore)) {
            ks.load(fis, pwdCharArr);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
        );
        kmf.init(ks, pwdCharArr);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
        );
        tmf.init((KeyStore) null);

        sslContext = SSLContext.getInstance(protocol);
        sslContext.init(
                kmf.getKeyManagers(),
                tmf.getTrustManagers(),
                null
        );

        enableSSL = true;
        throw new RuntimeException("暂时没有实现, 因为目前都是在用nginx代理");
    }

    public int getBufferCapacity() {
        return bufferCapacity;
    }

    /** 单个ByteBuffer大小, 默认16kb */
    public void setBufferCapacity(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    public boolean isEnableSSL() {
        return enableSSL;
    }

    public boolean isEnableWebSocket() {
        return !webSocketHandlers.isEmpty();
    }

    public String getUploadTmpDir() {
        return uploadTmpDir;
    }

    /** 会在调用start方法时向filter内注入bean */
    public void setFileFilter(HttpFileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public void setUploadTmpDir(String uploadTmpDir) {
        this.uploadTmpDir = uploadTmpDir;
    }

    public int getCacheContentToFileLimit() {
        return cacheContentToFileLimit;
    }

    /** http body 超过此值会使用文件缓存起来 */
    public void setCacheContentToFileLimit(int cacheContentToFileLimit) {
        this.cacheContentToFileLimit = cacheContentToFileLimit;
    }

    public String getDefaultIndexFile() {
        return defaultIndexFile;
    }

    public void setDefaultIndexFile(String defaultIndexFile) {
        this.defaultIndexFile = defaultIndexFile;
    }

    public void setKeepLiveRequestCount(int keepLiveRequestCount) {
        this.keepLiveRequestCount = keepLiveRequestCount;
    }

    public void setKeepLiveTimeout(int keepLiveTimeout) {
        server.setSessionIdleTime(keepLiveTimeout);
    }

    public NioTcpServer getNioTcpServer() {
        return server;
    }

    public void addStaticPath(String path) {
        if(Utils.CheckNull(path)) {
            throw new RuntimeException("path is empty");
        }
        staticPath.add(path);
    }

    public boolean isSupportSymbolicLink() {
        return isSupportSymbolicLink;
    }

    public void setSupportSymbolicLink(boolean supportSymbolicLink) {
        isSupportSymbolicLink = supportSymbolicLink;
    }

    public void addWebSocketMessageHandler(HttpWebSocketMessageHandler webSocketMessageHandler) {
        webSocketHandlers.put(webSocketMessageHandler.getPath(), webSocketMessageHandler);
        addBean(webSocketMessageHandler);
    }

    public void removeWebSocketMessageHandler(HttpWebSocketMessageHandler webSocketMessageHandler) {
        webSocketHandlers.remove(webSocketMessageHandler.getPath());

    }

    @Override
    protected void sendResponse(boolean isHandle, RestfulRequest request, RestfulResponse response) {
        HttpRequest httpRequest = (HttpRequest) request;
        HttpResponse httpResponse = (HttpResponse) response;
        if(!isHandle) {
            if(httpRequest.isWebSocket) {
                if(!webSocketHandlers.isEmpty()) {
                    HttpWebSocketMessageHandler handler = webSocketHandlers.get(request.getPath());
                    if(handler != null) {
                        httpRequest.session.setCodec(webSocketProtocolCodec);
                        //虽然会释放request对象, 但是websocket请求没有数据在body里面
                        WebSocketIoHandler wsHandler = new WebSocketIoHandler(handler);
                        httpRequest.session.setHandler(wsHandler);
                        try {
                            handler.onConnection(httpRequest, httpRequest.session);
                        } catch (Exception e) {
                            handler.onException(httpRequest.session, e);
                        }
                    } else {
                        response.setHeader(HttpHeaderNames.CONNECTION, "close");
                        response.removeHeader(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
                        response.removeHeader(HttpHeaderNames.SEC_WEBSOCKET_KEY);
                        response.removeHeader(HttpHeaderNames.UPGRADE);
                        response.setStatus(RestfulResponseStatus.CLIENT_ERROR_NOT_FOUND);
                    }
                    httpRequest.session.write(response);
                    return;
                }
            }

            //未被restful处理则走静态文件处理
            try {
                handleFileResponse(httpRequest, httpResponse);
            } catch (IOException e) {
                log.debug("处理文件出错:", e);
            }
        }
        if(response.getHeader(HttpHeaderNames.CONTENT_TYPE) == null) {
           response.setHeader(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=" + charset.displayName());
        }
        //发送失败时直接释放内存
        if(!httpRequest.session.write(httpResponse)) {
            httpResponse.close();
        }
        String connection = response.getHeader(HttpHeaderNames.CONNECTION);
        if("close".equals(connection) || Utils.CheckNull(connection)) {
            httpRequest.session.closeOnFlush();
        }
    }

    protected void handleFileResponse(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();

        if("/".equals(path)) {
            path = "/index.html";
        }
        if(fileFilter != null && fileFilter.before(request)) {
            response.setStatus(RestfulResponseStatus.CLIENT_ERROR_NOT_FOUND);
            return;
        }

        File file = null;

        if(fileFilter != null) {
            file = fileFilter.getFile(path, this, request);
        }
        if(file == null) {
            file = sanitizeUri(path);
        }
        if((fileFilter != null && fileFilter.request(file, request, response)) || file == null) {
            response.setStatus(RestfulResponseStatus.CLIENT_ERROR_NOT_FOUND);
            return;
        }
        if(file != null) {
            String web = request.getHeader(HttpHeaderNames.IF_MODIFIED_SINCE);
            String self = new Date(Files.getLastModifiedTime(file.toPath(), LinkOption.NOFOLLOW_LINKS).toMillis()).toString();
            response.setHeader(HttpHeaderNames.CACHE_CONTROL, "max-age=315360000");
            response.setHeader(HttpHeaderNames.LAST_MODIFIED, self);

            if(!Utils.CheckNull(web) && web.equals(self)) {
                //缓存没有变
                response.setStatus(RestfulResponseStatus.REDIRECTION_NOT_MODIFIED);
                return ;
            }
            //默认是text/html, 这里根据文件类型覆盖掉, 否则 浏览器识别会有问题
            response.setHeader(HttpHeaderNames.CONTENT_TYPE, RestfulUtils.getMimeType(charset.displayName(), file));
            response.write(file);
        }
    }

    /** 转换请求路径为本地路径, 返回 null 表示未启用本地路径或者转换失败 */
    private File sanitizeUri(String uri) {
        if(staticPath.isEmpty()) {
            return null;
        }
        if (uri == null || uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        try {

            for(String localPath : staticPath) {
                Path parent = Paths.get(localPath).normalize();
                Path reqPath = Paths.get(localPath, uri).normalize();
                if(!reqPath.startsWith(parent)) {
                    continue;
                }
                //支持符号链接

                File file = null;
                if(Files.isSymbolicLink(reqPath)) {
                    /**检查配置是否启用支持软链接*/
                    if(!isSupportSymbolicLink) {
                        //sendError(ctx, request, HttpResponseStatus.FORBIDDEN, this.server.getCharset());
                        continue;
                    }

                    reqPath = Files.readSymbolicLink(reqPath);
                    file = reqPath.toFile();
                } else {
                    file = reqPath.toFile();
                }

                if(file.isHidden() || !file.exists()) {
                    //sendError(ctx, request, HttpResponseStatus.NOT_FOUND, this.server.getCharset());
                    continue;
                }

                if (file.isDirectory()) {
                    //sendError(ctx, request, HttpResponseStatus.NOT_FOUND, this.server.getCharset());
                    continue;
                }

                if (!file.isFile()) {
                    //sendError(ctx, request, HttpResponseStatus.FORBIDDEN, this.server.getCharset());
                    continue;
                }
                return file;
            }

        } catch (Exception e) {
            //这里不报错
            //log.error("格式化本地路径出错: " + uri, e);
        }
        return null;
    }

    private class WebSocketIoHandler extends IoHandler {
        HttpWebSocketMessageHandler handler;

        public WebSocketIoHandler(HttpWebSocketMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceiveMessage(Session session, Object msg) throws Exception {
            handler.onMessage(session, (WebSocketFrame) msg);
        }

        @Override
        public void onException(Session session, Throwable e) {
            handler.onException(session, e);
        }

        @Override
        public void onIdle(Session session) throws Exception {
            handler.onIdle(session);
        }

        @Override
        public void onClose(Session session) throws Exception {
            handler.onClose(session);
        }

    }

    private class HttpIoHandler extends IoHandler {

        protected void freeSession(Session session) {
            //解码时有可能body收一半但是关闭了, 需要手动释放解码时的body
            HttpRequest request = (HttpRequest) session.removeAttr(HttpProtocolCodec.REQUEST);
            if(request != null) {
                request.close();
            }
        }

        @Override
        public void onIdle(Session session) throws Exception {
            freeSession(session);
            session.closeNow();
        }


        @Override
        public void onReceiveMessage(Session session, Object msg) throws Exception {
            HttpRequest request = (HttpRequest) msg;
            if(request != null) {
                HttpResponse response = null;
                try {
                    response = new HttpResponse(request);
                    dispatch(request, response);
                } catch (Throwable e) {
                    if(response != null) {
                        response.close();
                    }
                    throw new HttpServerException(500, request, e);
                } finally {
                    request.close();
                }
            }
        }

        @Override
        public void onClose(Session session) throws Exception {
            freeSession(session);
        }

        @Override
        public void onException(Session session, Throwable e) {
            //session.removeAttr(HttpProtocolCodec.REQUEST);
            session.removeAttr(HttpProtocolCodec.STATE);
            HttpRequest request = null;
            HttpResponse response = null;
            if(e instanceof HttpServerException) {
                request = ((HttpServerException) e).request;
                if(request != null) {
                    response = new HttpResponse(request);
                }
            }

            if(filter != null) {
                if(filter.exception(e, request, response)) {
                    return;
                }
            }
            //处理请求时发生的异常
            if(!session.isClosed() && response != null) {
                response.setStatus(RestfulResponseStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
                session.write(response);
                ((NioTcpSession) session).closeOnFlush();
                return;
            }

            session.closeNow();
        }
    }
}
