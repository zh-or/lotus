package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.nio.IoHandler;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.tcp.NioTcpServer;

import javax.net.ssl.SSLContext;

public class HttpServer extends RestfulContext {
    NioTcpServer server;
    private boolean enableWebSocket = false;
    private String uploadTmpDir = null;
    String defaultIndexFile = "index.html";
    private boolean enableSSL = false;
    private SSLContext sslContext = null;
    /** 如果http内容超过此值, 则缓存到文件 */
    private int cacheContentToFileLimit = 1024 * 1024 * 4;

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

    @Override
    protected void sendResponse(boolean isHandle, RestfulRequest request, RestfulResponse response) {
        HttpRequest httpRequest = (HttpRequest) request;
        HttpResponse httpResponse = (HttpResponse) response;

        httpRequest.session.write(httpResponse);
    }


    private class HttpIoHandler extends IoHandler {
        @Override
        public void onIdle(Session session) throws Exception { }

        @Override
        public void onReceiveMessage(Session session, Object msg) throws Exception {
            HttpRequest request = (HttpRequest) msg;
            dispatch(request, new HttpResponse(request));
        }

        @Override
        public void onSentMessage(Session session, Object msg) throws Exception {
            HttpResponse httpResponse = (HttpResponse) msg;
            if(httpResponse != null) {//发送完毕后调用关闭
                httpResponse.request.close();
                httpResponse.close();
            }
        }

        @Override
        public void onClose(Session session) throws Exception {

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
