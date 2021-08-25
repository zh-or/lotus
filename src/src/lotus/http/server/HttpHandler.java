package lotus.http.server;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Date;

import lotus.http.WebSocketFrame;
import lotus.http.server.support.HttpMethod;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.ResponseStatus;
import lotus.nio.Session;
import lotus.utils.Utils;


public abstract class HttpHandler {
    

    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) throws Exception {
        switch (mothed) {
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

    public void wsConnection(Session session, HttpRequest request) throws Exception{ }
    public void wsMessage(Session session,  HttpRequest request, WebSocketFrame frame) throws Exception{ }
    public void wsClose(Session session,  HttpRequest request) throws Exception{ }
    

    /**
     * 检查参数是否为空, 值可以为空, key不能为空
     * @param pars 参数key数组
     * @param request
     * @return 如果有为空的则返回 false
     */
    public boolean _checkparameter(String[] pars, HttpRequest request){
        if(pars == null || pars.length <= 0) return true;
        for(int i = 0; i < pars.length; i++){
            if(request.getParameter(pars[i]) == null){
                return false;
            }
        }
        return true;
    }
    
    /**
     * @param pars
     * @param val
     * @return 如果包含则返回true
     */
    public boolean _filterStrings(String[] pars, String val){
        if(pars == null || pars.length <= 0) return false;
        for(int i = 0; i < pars.length; i++){
            if(pars[i].equals(val)) return true;
        }
        return false;
    }
    
    public static String _filename2type(String pathname){
        if(pathname.indexOf(".js") != -1 ){
            return "application/javascript; charset=utf-8";
        }
        if(pathname.indexOf(".html") != -1 ){
            return "text/html; charset=utf-8";
        }
        if(pathname.indexOf(".gif") != -1 ){
            return "image/gif";
        }
        if(pathname.indexOf(".png") != -1 ){
            return "image/png";
        }
        if(pathname.indexOf(".jpg") != -1 ){
            return "image/jpg";
        }
        return "";
    }

    /*参数错误*/
    public static final int STATE_PARAMETER_ERROR       =   -3;
    /*服务器发生错误*/
    public static final int STATE_SERVER_ERROR          =   -2;
    /*其他错误*/
    public static final int STATE_ERROR                 =   -1;
    /*未登陆*/
    public static final int STATE_NOT_LOGIN             =   -4;
    /*成功*/
    public static final int STATE_RESPONSE_SUCCESS      =   1;
    
    public String _createResponse(int state, String data){
        if(data == null || data == "") data = "null";
        String res = "";
        switch (state) {
            case STATE_RESPONSE_SUCCESS:
                res = String.format("{\"state\":%d, \"message\":\"%s\", \"data\": %s}", state, "请求成功", data);
                break;
            case STATE_PARAMETER_ERROR:
                res = String.format("{\"state\":%d, \"message\":\"%s\", \"data\": %s}", state, "参数错误", data);
                break;
            case STATE_SERVER_ERROR:
                res = String.format("{\"state\":%d, \"message\":\"%s\", \"data\": %s}", state, "服务器发生错误", data);
                break;
            case STATE_NOT_LOGIN:
                data = "请先登录";
            case STATE_ERROR:
                res = String.format("{\"state\":%d, \"message\":\"%s\", \"data\": %s}", state, data, "null");
                break;
        }
        return res;
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
     * @param request
     * @param response
     * @return 返回 true 表示已返回文件
     * @throws Exception 
     */
    public boolean defFileRequest(String basePath, HttpRequest request, HttpResponse response) throws Exception {
        String reqPath  = request.getPath();
        String filePath = basePath + Utils.BuildPath(reqPath);
        File file = new File(filePath);
        
        if(!file.exists()) {
            response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
            // 404
            return false;
        }
        String web = request.getHeader("if-modified-since");
        String self = new Date(Files.getLastModifiedTime(file.toPath(), LinkOption.NOFOLLOW_LINKS).toMillis()).toString();
        response.setHeader("Cathe-Control", "max-age=315360000");
        response.setHeader("Last-Modified", self);
        
        if(!Utils.CheckNull(web) && web.equals(self)) {
            //缓存没有变
            response.setStatus(ResponseStatus.REDIRECTION_NOT_MODIFIED);
            return true;
        }
        boolean useSendFile = file.length() > 1024 * 1024;
        response.setHeader("Content-Type", _filename2type(filePath));
        if(useSendFile) {
            response.sendFile(file);
        } else {
            try (FileInputStream fileIn = new FileInputStream(file);){
                //编写文件下载
                byte[] filedata = new byte[(int) file.length()];
                fileIn.read(filedata);
                response.write(filedata);
            }
        }
        return true;
        
    }
}
