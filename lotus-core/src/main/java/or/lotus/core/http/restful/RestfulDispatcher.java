package or.lotus.core.http.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.Parameter;
import or.lotus.core.http.restful.support.ModelAndView;
import or.lotus.core.http.restful.support.PostBodyType;
import or.lotus.core.http.restful.support.RestfulHttpMethod;
import or.lotus.core.http.restful.support.RestfulUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
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

    public void dispatch(RestfulContext context, RestfulRequest request, RestfulResponse response) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] params = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++ ) {
            Object o = handleParameter(context, request, response, parameterTypes[i]);
            if(o != null) {
                params[i] = o;
            }
        }

        Object ret = method.invoke(controllerObject, params);

        if(ret == null) {

        } else if(ret instanceof ModelAndView) {
            if(context.templateEngine == null) {
                throw new IllegalStateException("你返回了ModelAndView, 但是并没有启用模板引擎.");
            }
            ModelAndView mv = (ModelAndView) ret;

            if(mv.isRedirect) {//302跳转
                response.redirect(mv.getViewName());
            } else {
                try {
                    context.templateEngine.process(
                            mv.getViewName(),
                            mv.values,
                            response
                    );
                } catch(Exception e) {
                    if (context.filter != null) {
                        context.filter.exception(e, request, response);
                    } else {
                        log.error("处理模板出错:", e);
                    }
                    return ;
                }

                if(context.outModelAndViewTime) {
                    try {
                        response.write("<!-- handle time: " + ((System.nanoTime() - mv.createTime) / 1_000_000) + "ms -->");
                    } catch (IOException e) {}
                }
                response.setHeader("Content-Type", "text/html; charset=" + response.charset.displayName());
            }
        } else {
            response.write(ret.toString());
        }
    }

    private Object handleParameter(RestfulContext context, RestfulRequest request, RestfulResponse response, Class<?> type) throws JsonProcessingException {
        if(type.isInstance(request)) {
            return request;
        }

        if(type.isInstance(response)) {
            return response;
        }

        if(type.isInstance(context)) {
            return context;
        }

        Parameter parameter = type.getAnnotation(Parameter.class);
        if(parameter != null) {
            String name = parameter.value();
            RestfulHttpMethod reqMethod = request.getMethod();

            if(Utils.CheckNull(name)) {
                if(reqMethod == RestfulHttpMethod.POST && request.getPostBodyType() == PostBodyType.JSON) {
                    /** post json 并且参数是对象尝试直接转换 */
                    if(!RestfulUtils.isBaseType(type)) {
                        return BeanUtils.JsonToObj(type, request.getBodyString());
                    }
                }
                return null;
            } else  {
                /**只支持基本数据类型*/
                if(reqMethod == RestfulHttpMethod.GET
                        || reqMethod == RestfulHttpMethod.DELETE
                        || reqMethod == RestfulHttpMethod.OPTIONS
                        || (reqMethod == RestfulHttpMethod.POST && request.getPostBodyType() == PostBodyType.URLENCODED)
                ) {
                    String val = request.getParameter(name);
                    if(val == null) {
                        return null;
                    }

                    if(type.isArray()) {
                        return RestfulUtils.valueToArray(
                                type.getComponentType(),
                                val.split(","));
                    }
                    if(type.isAssignableFrom(List.class)) {
                        Class childType = (Class) ((ParameterizedType) type.getGenericSuperclass()).getActualTypeArguments()[0];
                        Object[] res = RestfulUtils.valueToArray(
                                childType,
                                val.split(","));
                        return Arrays.asList(res);
                    }
                    return RestfulUtils.valueToType(type, val);
                } else if(reqMethod == RestfulHttpMethod.POST && request.getPostBodyType() == PostBodyType.JSON) {

                    return RestfulUtils.valueToType(type, request.getJsonNodeForPath(name).asText());
                }
            }
        }
        return null;
    }


}
