package lotus.http.server;

import java.lang.reflect.Method;

/**
 * 此handler为模仿web用的 加个m参数则调用子类的m方法, (m为方法名称)
 * 如 url: http://127.0.0.1/?m=test&name=1&password=123
 * 则为调用过滤器过滤后的类的test方法
 * 此类定义一定要为public private会调用失败
 * @author or
 *
 */
public abstract class ExHttpHandler extends HttpHandler{
    
    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) {
        String m = request.getParameter("m");
        try{
            if(m == null || m.trim() == ""){
                this._do(request, response);
                return;
            }
            if(!this._check(m, request, response)){
                return;
            }
        }catch(Throwable e){
            this.exception(e, request, response);
            return;
        }
        Method method = null;
        try {
            method = this.getClass().getMethod(m, HttpRequest.class, HttpResponse.class);
        } catch (Throwable e) {
            this.exception(e, request, response);
        }
        if(method != null){
            try {
                method.invoke(this, request, response);
            } catch (Throwable e) {
                this.exception(e, request, response);
            }
        }
    }
    
    /**
     * 如果未指定参数m,默认调用此方法
     * @param request
     * @param response
     */
    public abstract void _do(HttpRequest request, HttpResponse response) throws Exception;
    
    /**
     * 执行方法前将首先调用此函数 用于各种检查
     * @param request
     * @param response
     * @return 返回 true 时则调用后面的函数, 否则则直接返回
     */
    public boolean _check(String method, HttpRequest request, HttpResponse response) throws Exception{
        
        return true;
    }
    
    

    
    public void tmp(HttpRequest request, HttpResponse response) throws Exception{}
}
