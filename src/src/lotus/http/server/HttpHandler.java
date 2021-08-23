package lotus.http.server;

import lotus.http.WebSocketFrame;
import lotus.http.server.support.HttpMethod;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.ResponseStatus;
import lotus.nio.Session;


public abstract class HttpHandler {
    

    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) {
        try{
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
        }catch(Throwable e){
            exception(e, request, response);
        }
    }
    
    public void get(HttpRequest request, HttpResponse response) throws Exception{}
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
    public void exception(Throwable e, HttpRequest request, HttpResponse response){
        response.setStatus(ResponseStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
        e.printStackTrace();
    }
}
