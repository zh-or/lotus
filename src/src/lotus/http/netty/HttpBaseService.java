package lotus.http.netty;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;


public abstract class HttpBaseService {

    private ConcurrentHashMap<String, Method> __funs;
    private HttpServer context;

    public HttpBaseService() {
        Method[] ms = this.getClass().getDeclaredMethods();
        __funs = new ConcurrentHashMap<>(ms.length);

        for(Method m : ms) {
            HttpServicePath map = m.getAnnotation(HttpServicePath.class);
            if(map != null){
                __funs.put(map.value(), m);
            }
        }
    }

    public void __setContext(HttpServer context) {
        this.context = context;
    }

    /**
     * 请勿覆盖此方法
     * @param path
     * @return
     */
    public Object __dispatch(String path, HttpRequestPkg request) throws RequestPathNotFound {
        Method m = __funs.get(path);
        if(m != null) {

            try {
                if(filter(path, request)) {
                    return m.invoke(this, request);
                }
            } catch(Throwable e) {
                return context.exception(e, request);
            }
        }
        throw new RequestPathNotFound(path + " not found");
    }

    /**
     * 重写此方法 返回false可拦截调用
     * @param path
     * @param request
     * @return
     */
    public boolean filter(String path, HttpRequestPkg request) {
        return true;
    }
}
