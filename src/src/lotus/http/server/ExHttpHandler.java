package lotus.http.server;

import java.lang.reflect.Method;

import lotus.util.Util;

/**
 * 此handler为模仿web用的 加个m参数则调用子类的m方法, (m为方法名称)
 * 如 url: http://127.0.0.1/?m=test&name=1&password=123
 * 则为调用过滤器过滤后的类的test方法
 * @author or
 *
 */
public abstract class ExHttpHandler extends HttpHandler{
    
    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) throws Exception {
        String m = request.getParameter("m");
        try{
            if(m == null || m.trim() == ""){
                this._do(request, response);
                return;
            }
            if(!this._check(m, request, response)){
                return;
            }
        }catch(Exception e){
            this._exception(e, request, response);
            return;
        }
        Method method = null;
        try {
            method = this.getClass().getMethod(m, HttpRequest.class, HttpResponse.class);
        } catch (Exception e) {
            this._exception(e, request, response);
        }
        if(method != null){
            try {
                method.invoke(this, request, response);
            } catch (Exception e) {
                this._exception(e, request, response);
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
    
    /**
     * 检查参数是否为空
     * @param pars 参数key数组
     * @param request
     * @return 如果有为空的则返回 false
     */
    public boolean _checkparameter(String[] pars, HttpRequest request){
        if(pars == null || pars.length <= 0) return true;
        for(int i = 0; i < pars.length; i++){
            if(Util.CheckNull(request.getParameter(pars[i]))){
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
    
    /**
     * 如果反射发生了错误 则会调用子类实现的此方法
     * @param e
     * @param request
     * @param response
     */
    public void _exception(Exception e, HttpRequest request, HttpResponse response){
        e.printStackTrace();
    }
    
    public void tmp(HttpRequest request, HttpResponse response) throws Exception{}
}
