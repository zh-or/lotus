package or.lotus.common.http.simpleserver;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import or.lotus.common.http.simpleserver.support.HttpRequest;
import or.lotus.common.http.simpleserver.support.HttpResponse;
import or.lotus.common.http.simpleserver.support.HttpServicePath;


public abstract class HttpBaseService {

    private ConcurrentHashMap<String, Method> __funs;
    private HttpRestServiceDispatcher         __dispatcher;

    public HttpBaseService() {
        Method[] ms = this.getClass().getDeclaredMethods();
        __funs = new ConcurrentHashMap<>(ms.length);

        for(Method m : ms) {
            HttpServicePath map = m.getAnnotation(HttpServicePath.class);
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
                if(filter(path, request, response)) {
                    m.invoke(this, request, response);
                }
            } catch(Throwable e) {
                __dispatcher.exception(e, request, response);
            }
            return true;
        }
        return false;
    }

    /**
     * 重写此方法 返回false可拦截调用
     * @param path
     * @param request
     * @param response
     * @return
     */
    public boolean filter(String path, HttpRequest request, HttpResponse response) {
        return true;
    }
}
