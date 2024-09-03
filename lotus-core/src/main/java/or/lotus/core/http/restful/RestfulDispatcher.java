package or.lotus.core.http.restful;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.Autowired;
import or.lotus.core.http.restful.ann.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class RestfulDispatcher {
    static final Logger log = LoggerFactory.getLogger(RestfulDispatcher.class);
    public String url;
    public String dispatcherUrl;
    public boolean isPattern;
    public Object controllerObject;
    public Method method;
    public RestfulHttpMethod httpMethod;

    public RestfulDispatcher(String url, Object controllerObject, Method method, RestfulHttpMethod httpMethod, boolean isPattern) {
        this.url = url.replaceAll("//", "/");
        this.dispatcherUrl = url + httpMethod.name();
        this.controllerObject = controllerObject;
        this.method = method;
        this.httpMethod = httpMethod;
        this.isPattern = isPattern;
    }

    public boolean checkPattern(RestfulRequest request) {
        if(!isPattern) {
            return false;
        }

        return Pattern.matches(url, request.getUrl());
    }

    public RestfulResponse dispatch(RestfulContext context, RestfulRequest request) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] params = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++ ) {
            Object o = handleParameter(context, request, parameterTypes[i]);
            if(o != null) {
                params[i] = o;
            }
        }

        Object ret = method.invoke(controllerObject, params);
        //todo 处理返回值为 response
        // modelAndView -> response
        // string -> response
        // json -> response
        // object.toString() -> response
        return null;
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
