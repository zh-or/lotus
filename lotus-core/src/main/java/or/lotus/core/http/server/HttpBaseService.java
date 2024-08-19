package or.lotus.core.http.server;

import or.lotus.core.http.server.exception.HttpRequestPathNotFoundException;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public abstract class HttpBaseService {

    private ConcurrentHashMap<String, Method> __funs;
    private ConcurrentHashMap<String, Method> __funs_reg;
    private HttpServer context;

    public HttpBaseService() {
        Method[] ms = this.getClass().getDeclaredMethods();
        __funs = new ConcurrentHashMap<>(ms.length);
        __funs_reg = new ConcurrentHashMap<>(ms.length);

        for(Method m : ms) {
            HttpServicePath map = m.getAnnotation(HttpServicePath.class);
            if(map != null) {
                //简单处理路径最后带参数的情况
                String path = map.value();
                if(map.reg()) {
                    __funs_reg.put(path, m);
                } else {
                    __funs.put(path, m);
                }
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
    public Object __dispatch(String path, HttpRequestPkg request) throws HttpRequestPathNotFoundException {
        Method m = __funs.get(path);

        if(m == null) {
            Iterator<Map.Entry<String, Method>> it =__funs_reg.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String, Method> entry = it.next();
                String key = entry.getKey();

                if(Pattern.matches(key, path)) {
                    m = entry.getValue();
                    break;
                }
            }
        }

        if(m != null) {
            try {
                Object k = filter(path, request);
                if(k == null) {
                    k = filterMethod(path, request, m);
                }
                if(k == null) {
                    k = m.invoke(this, request);
                }
                return buildResult(path, request, k);
            } catch(Throwable e) {
                return context.exception(e, request);
            }
        }
        throw new HttpRequestPathNotFoundException(path + " not found");
    }

    /**
     * 重写此方法 返回null表示不处理, 返回不为空则拦截并返回到客户端
     * 优先级比filterMethod高
     * @param path
     * @param request
     * @return
     */
    public Object filter(String path, HttpRequestPkg request) throws Exception{
        return null;
    }

    /**
     * 方法调用前执行
     * @param path
     * @param request
     * @param m
     * @return
     */
    public Object filterMethod(String path, HttpRequestPkg request, Method m) throws Exception {
        return null;
    }

    /**
     * 重写此方法统一处理返回值
     * @param path
     * @param request
     * @param obj
     * @return
     * @throws Exception
     */
    public Object buildResult(String path, HttpRequestPkg request, Object obj) throws Exception {
        return obj;
    }

}
