package lotus.http.server;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.HttpServiceMap;


public abstract class HttpBaseService {
    
    private ConcurrentHashMap<String, Method> __funs;
    private HttpRestServiceDispatcher         __dispatcher;
    
    public HttpBaseService() {
        Method[] ms = this.getClass().getDeclaredMethods();
        __funs = new ConcurrentHashMap<>(ms.length);
        
        for(Method m : ms){
            HttpServiceMap map = m.getAnnotation(HttpServiceMap.class);
            if(map != null){
                __funs.put(map.path(), m);
            }
        }
    }

    public void __setDispathcher(HttpRestServiceDispatcher dispatcher) {
        this.__dispatcher = dispatcher;
    }
    
    /**
     * 请勿覆盖此方法
     * @param path
     * @return
     */
    public boolean __dispatch(String path, HttpRequest request, HttpResponse response) {
        Method m = __funs.get(path);
        if(m != null) {
            try {
                m.invoke(this, request, response);
            } catch(Exception e) {
                __dispatcher.exception(e, request, response);
            }
            return true;
        }
        return false;
    }
    
}
