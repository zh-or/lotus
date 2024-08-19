package or.lotus.core.http.simpleserver;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Date;

import or.lotus.core.http.simpleserver.support.HttpMethod;
import or.lotus.core.http.simpleserver.support.HttpRequest;
import or.lotus.core.http.simpleserver.support.HttpResponse;
import or.lotus.core.http.simpleserver.support.ResponseStatus;
import or.lotus.core.http.WebSocketFrame;
import or.lotus.core.nio.Session;
import or.lotus.core.common.Utils;


public abstract class HttpHandler {


    public void service(HttpMethod methed, HttpRequest request, HttpResponse response) throws Exception {
        switch (methed) {
            case GET:
                this.get(request, response);
                break;
            case POST:
                this.post(request, response);
                break;
            case CONNECT:
                this.connect(request, response);
                break;
            case DELETE:
                this.delete(request, response);
                break;
            case HEAD:
                this.head(request, response);
                break;
            case OPTIONS:
                this.options(request, response);
                break;
            case PUT:
                this.put(request, response);
                break;
            case TRACE:
                this.trace(request, response);
                break;
        }

    }

    public void get(HttpRequest request, HttpResponse response) throws Exception {
        defFileRequest("./", request, response);
    }

    public void post(HttpRequest request, HttpResponse response) throws Exception{}
    public void connect(HttpRequest request, HttpResponse response) throws Exception{}
    public void delete(HttpRequest request, HttpResponse response) throws Exception{}
    public void head(HttpRequest request, HttpResponse response) throws Exception{}
    public void options(HttpRequest request, HttpResponse response) throws Exception{}
    public void put(HttpRequest request, HttpResponse response) throws Exception{}
    public void trace(HttpRequest request, HttpResponse response) throws Exception{}

    /**
     *
     * @param session
     * @param request
     * @return 返回 true 表示不拦截, 返回 false 表示拦截该请求并断开该链接
     * @throws Exception
     */
    public boolean wsConnection(Session session, HttpRequest request) throws Exception{
        return true;
    }

    /**
     * 注意此方法不会收到客户端发过来的 OP_CLOSE 消息, 内部已经处理了此消息
     * @param session
     * @param request
     * @param frame
     * @throws Exception
     */
    public void wsMessage(Session session,  HttpRequest request, WebSocketFrame frame) throws Exception{ }
    public void wsClose(Session session,  HttpRequest request) throws Exception{ }
    public void wsIdle(Session session,  HttpRequest request) throws Exception {
        session.closeNow();
    }
    /**
     * 当调用 defFileRequest 时触发此回调
     * @param path
     * @param request
     * @param response
     * @return 返回 true 表示拦截默认文件处理
     * @throws Exception
     */
    public boolean requestFile(String path, HttpRequest request, HttpResponse response) throws Exception {
        return false;
    }

    /**
     * 如果反射发生了错误 则会调用子类实现的此方法
     * @param e
     * @param request
     * @param response
     */
    public void exception(Throwable e, HttpRequest request, HttpResponse response) {
        if(response != null) {
            response.setStatus(ResponseStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
        }
        e.printStackTrace();
    }

    /**
     * 处理静态文件请求, 大于1M的文件将使用 response.sendFile 发送, 需要注意的是此方法最大能发送2G的文件
     * @param basePath
     * @param request 如果请求路径为 / 则会默认转换为 /index.html
     * @param response
     * @return 返回 true 表示已返回文件
     * @throws Exception
     */
    public boolean defFileRequest(String basePath, HttpRequest request, HttpResponse response) throws Exception {

        if(Utils.CheckNull(basePath)) {
            return false;
        }

        String reqPath  = request.getPath();

        if(this.requestFile(reqPath, request, response)) {
            return true;
        }

        if("/".equals(reqPath)) {
            reqPath = "/index.html";
        }
        String filePath = basePath + Utils.BuildPath(reqPath);
        File file = new File(filePath);

        if(!file.exists()) {
            response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
            // 404
            return false;
        }

        String web = request.getHeader("if-modified-since");
        String self = new Date(Files.getLastModifiedTime(file.toPath(), LinkOption.NOFOLLOW_LINKS).toMillis()).toString();
        response.setHeader("Cache-Control", "max-age=315360000");
        response.setHeader("Last-Modified", self);

        if(!Utils.CheckNull(web) && web.equals(self)) {
            //缓存没有变
            response.setStatus(ResponseStatus.REDIRECTION_NOT_MODIFIED);
            return true;
        }
        boolean useSendFile = file.length() > 1024 * 1024;
        response.setHeader("Content-Type", HttpResponse.filename2type(filePath));
        response.setStatus(ResponseStatus.SUCCESS_OK);
        if(useSendFile) {
            response.sendFile(file);
        } else {
            try (FileInputStream fileIn = new FileInputStream(file);){
                byte[] filedata = new byte[(int) file.length()];
                fileIn.read(filedata);
                response.write(filedata);
            }
        }
        return true;

    }
}
