package or.lotus.core.http.rest;

import or.lotus.core.common.Utils;
import or.lotus.core.http.rest.ann.Autowired;
import or.lotus.core.http.rest.ann.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RestfulDispatcher {
    static final Logger log = LoggerFactory.getLogger(RestfulDispatcher.class);
    public String url;
    public boolean isPattern;
    public Class<?> clazz;
    public Method method;
    public RestfulHttpMethod httpMethod;

    public RestfulDispatcher(String url, Class<?> clazz, Method method, RestfulHttpMethod httpMethod, boolean isPattern) {
        this.url = url.replaceAll("//", "/");
        this.clazz = clazz;
        this.method = method;
        this.httpMethod = httpMethod;
        this.isPattern = isPattern;
    }


    public Object dispatch(RestfulContext context, RestfulRequest request) throws Exception {
        Object ctl = clazz.newInstance();
        //注入Bean

        Field[] fields = clazz.getFields();

        for(Field field : fields) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if(autowired != null) {
                String name = autowired.value();
                if(Utils.CheckNull(name)) {
                    //使用全限定名
                    name = field.getDeclaringClass().getName();
                }

                Object bean = context.beansCache.get(name);
                if(bean != null) {
                    field.setAccessible(true);
                    field.set(ctl, bean);
                } else {
                    log.trace("{} 未初始化Bean", name);
                }
            }
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] params = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++ ) {
            Object o = handleParameter(context, request, parameterTypes[i]);
            if(o != null) {
                params[i] = o;
            }
        }

        return method.invoke(ctl, params);
    }

    private Object handleParameter(RestfulContext context, RestfulRequest request, Class<?> type) {
        if(type.isInstance(request)) {
            return request;
        }

        if(type.isInstance(context)) {
            return context;
        }

        Parameter parameter = type.getAnnotation(Parameter.class);
        if(parameter != null) {
            String name = parameter.value();

            if(!Utils.CheckNull(name)) {
                //todo 从request获取参数
            }
        }
        return null;
    }
}
