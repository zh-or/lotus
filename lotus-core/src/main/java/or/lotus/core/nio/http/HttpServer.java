package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.nio.IoHandler;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.tcp.NioTcpServer;

import javax.net.ssl.SSLContext;

public class HttpServer extends RestfulContext {
    protected NioTcpServer server;
    protected boolean enableWebSocket = false;
    protected String uploadTmpDir = null;
    protected String defaultIndexFile = "index.html";
    protected boolean enableSSL = false;
    protected SSLContext sslContext = null;
    /** 如果http内容超过此值, 则缓存到文件 */
    protected int cacheContentToFileLimit = 1024 * 1024 * 4;

    /** 每个连接最大请求数量 */
    protected int keepLiveRequestCount = 10;

    public HttpServer() {
        server = new NioTcpServer();
        server.setHandler(new HttpIoHandler());
        server.setProtocolCodec(new HttpProtocolCodec(this));
        server.setBufferCapacity(1024 * 16);
    }

    @Override
    protected void onStart() throws Exception {
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

    public boolean isEnableSSL() {
        return enableSSL;
    }

    public boolean isEnableWebSocket() {
        return enableWebSocket;
    }

    public String getUploadTmpDir() {
        return uploadTmpDir;
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

    @Override
    protected void sendResponse(boolean isHandle, RestfulRequest request, RestfulResponse response) {
        HttpRequest httpRequest = (HttpRequest) request;
        HttpResponse httpResponse = (HttpResponse) response;

        httpRequest.session.write(httpResponse);
    }


    private class HttpIoHandler extends IoHandler {
        @Override
        public void onIdle(Session session) throws Exception {
            //todo 空闲时说明超时了直接关闭
            log.info("超时: {}", session.getId());
            session.closeNow();
        }

        @Override
        public void onReceiveMessage(Session session, Object msg) throws Exception {
            if(msg != null) {
                HttpRequest request = (HttpRequest) msg;
                log.info("request: {}, path: {}", session.getId(), request.getPath());
                dispatch(request, new HttpResponse(request));
            }
        }

        @Override
        public void onSentMessage(Session session, Object msg) throws Exception {
            //todo keeplive时一个连接最多处理请求数量问题
            if(msg instanceof HttpSyncResponse) {
                HttpSyncResponse syncResponse = (HttpSyncResponse) msg;
                if(syncResponse.isEnd) {
                    //发送完毕了
                    //session.closeNow();
                }
            } else {
                HttpResponse httpResponse = (HttpResponse) msg;
                if(httpResponse != null) {//发送完毕后调用关闭

                    httpResponse.request.close();
                    httpResponse.close();
                    if(httpResponse.isOpenSync) {

                    } else {

                    }
                }
            }
        }

        @Override
        public void onClose(Session session) throws Exception {
            log.info("close: {}", session.getId());
        }

        @Override
        public void onException(Session session, Throwable e) {
            if(e instanceof HttpServerException) {
                e.printStackTrace();
                //todo 需要发送response
            } else {
                log.error("server exception:", e);
            }
        }
    }
}
