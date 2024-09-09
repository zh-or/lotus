package or.lotus.core.http.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.Attr;
import or.lotus.core.http.restful.ann.Parameter;
import or.lotus.core.http.restful.support.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static or.lotus.core.http.restful.ann.Parameter.DEF_NULL_VALUE;

public class RestfulDispatcher {
    static final Logger log = LoggerFactory.getLogger(RestfulDispatcher.class);
    public String url;
    public boolean isPattern;
    public RestfulHttpMethod httpMethod;
    public Object controllerObject;
    public Method method;
    private Class[] parameterTypes;
    private Parameter[] parameterAnnotations;
    private Attr[] attrs;
    private Class[] genericTypes;

    public RestfulDispatcher(String url, Object controllerObject, Method method, RestfulHttpMethod httpMethod, boolean isPattern) {
        this.url = url.replaceAll("//", "/");
        this.httpMethod = httpMethod;
        this.isPattern = isPattern;

        this.controllerObject = controllerObject;
        this.method = method;

        parameterTypes = method.getParameterTypes();
        Annotation[][] parameterTypesAnnotations = method.getParameterAnnotations();
        int size = parameterTypes.length;
        parameterAnnotations = new Parameter[size];
        genericTypes = new Class[size];
        attrs = new Attr[size];

        java.lang.reflect.Parameter[] parameters = method.getParameters();

        for(int i = 0; i < size; i++) {
            parameterAnnotations[i] = null;//参数名字注解
            for(Annotation ann : parameterTypesAnnotations[i]) {
                if(ann.annotationType() == Parameter.class) {
                    parameterAnnotations[i] = (Parameter) ann;
                    break;
                }
            }

            genericTypes[i] = null;//泛型类型
            java.lang.reflect.Type gt = parameters[i].getParameterizedType();
            if(gt != null && gt instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) gt;
                java.lang.reflect.Type[] actualTypeArguments = pt.getActualTypeArguments();

                if(actualTypeArguments != null & actualTypeArguments.length > 0) {
                    genericTypes[i] = (Class) actualTypeArguments[0];//取第一个
                }
            }

            attrs[i] = null;//参数名字注解
            for(Annotation ann : parameterTypesAnnotations[i]) {
                if(ann.annotationType() == Attr.class) {
                    attrs[i] = (Attr) ann;
                    break;
                }
            }
        }
    }

    /** 检查当前正则是否匹配url */
    public boolean checkPattern(RestfulRequest request) {
        if(!isPattern) {
            return false;
        }

        return Pattern.matches(url, request.getUrl());
    }

    public Object dispatch(RestfulContext context, RestfulRequest request, RestfulResponse response) throws Exception {
        Object[] params = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++ ) {

            try {
                params[i] = handleParameter(
                        context,
                        request,
                        response,
                        parameterTypes[i],
                        genericTypes[i],
                        parameterAnnotations[i],
                        attrs[i]);
            } catch (Throwable e) {
                throw new RuntimeException("处理参数出错:" + controllerObject + "\n" + method, e);
            }
        }

        return method.invoke(controllerObject, params);
    }

    private Object handleParameter(RestfulContext context,
                                   RestfulRequest request,
                                   RestfulResponse response,
                                   Class<?> type,
                                   Class childType,//泛型类型
                                   Parameter parameter,
                                   Attr attr) throws JsonProcessingException {

        //Parameter parameter = type.getAnnotation(Parameter.class);
        if(parameter != null) {
            String name = parameter.value();
            RestfulHttpMethod reqMethod = request.getMethod();

            if(Utils.CheckNull(name)) {
                /** post-> json 直接转换为对象 */
                if(reqMethod == RestfulHttpMethod.POST && request.getPostBodyType() == PostBodyType.JSON) {
                    String bodyString = request.getBodyString();
                    if(Utils.CheckNull(bodyString)) {
                        return null;
                    }
                    if(type.isAssignableFrom(List.class)) {
                        return BeanUtils.JsonToObj(
                                new TypeReferenceDynamic<List>(List.class, childType),
                                bodyString);
                    }

                    return BeanUtils.JsonToObj(type, bodyString);

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
                        val = parameter.def();
                        if(val == DEF_NULL_VALUE) {
                            return null;
                        }
                    }

                    if(type.isArray()) {
                        return  RestfulUtils.valueToArray(
                                type.getComponentType(),
                                val.split(","));
                    }
                    if(type.isAssignableFrom(List.class)) {

                        Object[] res = RestfulUtils.valueToArray(
                                childType,
                                val.split(","));
                        return Arrays.asList(res);
                    }
                    return RestfulUtils.valueToType(type, val);
                } else if(reqMethod == RestfulHttpMethod.POST && request.getPostBodyType() == PostBodyType.JSON) {
                    /** post->json 取其中的字段 */
                    JsonNode val = request.getJsonNodeForPath(name);
                    if(val.isMissingNode()) {
                        String defVal = parameter.def();

                        if(defVal == DEF_NULL_VALUE) {
                            return null;
                        }
                        /** 基本数据类型 */
                        if(RestfulUtils.isBaseType(type)) {
                            //json取不出来则取默认值
                            return RestfulUtils.valueToType(type, defVal);
                        }
                        return null;
                    }
                    /** 基本数据类型 */
                    if(RestfulUtils.isBaseType(type)) {
                        //json取不出来则取默认值
                        return RestfulUtils.valueToType(type, val.asText());
                    }
                    /** list */
                    if(type.isAssignableFrom(List.class)) {
                        //todo 需要测试

                        return BeanUtils.OBJECT_MAPPER.readValue(
                                val.toString(),
                                new TypeReferenceDynamic<List>(List.class, childType));
                    }
                    /** 对象 */
                    return BeanUtils.JsonToObj(type, val.toString());
                }
            }
        }
        if(attr != null) {
            //attr 的 value 必填
            return request.getAttribute(attr.value());
        } else {
            if(type.isInstance(request)) {
                return request;
            }

            if(type.isInstance(response)) {
                return response;
            }

            if(type.isInstance(context)) {
                return context;
            }

        }
        return null;
    }


}
