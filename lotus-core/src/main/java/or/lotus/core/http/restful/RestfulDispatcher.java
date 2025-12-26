package or.lotus.core.http.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.*;
import or.lotus.core.http.restful.support.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static or.lotus.core.http.restful.ann.Parameter.DEF_NULL_VALUE;

public class RestfulDispatcher {
    static final Logger log = LoggerFactory.getLogger(RestfulDispatcher.class);
    public String url;
    public RestfulHttpMethod httpMethod;
    public Object controllerObject;
    public Method method;
    private Class<?> returnType;
    private Class[] parameterTypes;

    private Annotation annotations[];

    private Class[] genericTypes;


    public RestfulDispatcher(String url, Object controllerObject, Method method, RestfulHttpMethod httpMethod) {
        this.url = url.replaceAll("//", "/");
        this.httpMethod = httpMethod;

        this.controllerObject = controllerObject;
        this.method = method;

        returnType = method.getReturnType();

        parameterTypes = method.getParameterTypes();
        Annotation[][] parameterTypesAnnotations = method.getParameterAnnotations();
        int size = parameterTypes.length;
        annotations = new Annotation[size];
        genericTypes = new Class[size];


        java.lang.reflect.Parameter[] parameters = method.getParameters();

        for(int i = 0; i < size; i++) {
            annotations[i] = null;

            for(Annotation ann : parameterTypesAnnotations[i]) {
                Class t = ann.annotationType();

                if(t == Parameter.class
                   || t == Autowired.class
                   || t == Attr.class
                   || t == PathVar.class
                   || t == Header.class
                   || t == Prop.class) {
                    annotations[i] = ann;
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
        }


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
                        annotations[i]);
            } catch (Throwable e) {
                throw new ParseParamsException("处理参数出错:" + controllerObject + "\n" + method, e);
            }
        }

        return method.invoke(controllerObject, params);
    }

    private Object handleParameter(RestfulContext context,
                                   RestfulRequest request,
                                   RestfulResponse response,
                                   Class<?> type,
                                   Class childType,//泛型类型
                                   Annotation annotation) throws JsonProcessingException {
        if(annotation != null) {
            Class annotationType = annotation.annotationType();
            if(annotationType == Parameter.class) {
                Parameter parameter = (Parameter) annotation;
                if(parameter != null) {
                    String name = parameter.value();
                    RestfulHttpMethod reqMethod = request.getMethod();

                    if(Utils.CheckNull(name)) {
                        /** 基本数据类型 */
                        if(RestfulUtils.isBaseType(type)) {
                            return RestfulUtils.valueToType(type, request.getBodyString());
                        }
                        /** post-> json 直接转换为对象 */
                        PostBodyType contentType = request.getPostBodyType();
                        if(reqMethod == RestfulHttpMethod.POST && (contentType == PostBodyType.JSON || contentType == PostBodyType.TEXT)) {
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
                                if(Objects.equals(val, DEF_NULL_VALUE)) {
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
                                if(Objects.equals(defVal, DEF_NULL_VALUE)) {
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
                                return BeanUtils.OBJECT_MAPPER.readValue(
                                        val.toString(),
                                        new TypeReferenceDynamic<List>(List.class, childType));
                            }
                            /** 对象 */
                            return BeanUtils.JsonToObj(type, val.toString());
                        }
                    }
                }
            } else if(annotationType == Attr.class) {
                Attr attr = (Attr) annotation;
                return request.getAttribute(attr.value());
            } else if(annotationType == Prop.class) {
                Prop prop = (Prop) annotation;
                return RestfulUtils.getPropObject(context, type, prop);
            } else if(annotationType == Autowired.class) {
                Autowired autowired = (Autowired) annotation;
                String name = autowired.value();
                if(Utils.CheckNull(name)) {
                    //使用全限定名
                    name = type.getName();
                }
                Object bean = context.getBean(name);
                return bean;
            } else if(annotationType == Header.class) {
                Header header = (Header) annotation;
                String headerName = header.value();
                return RestfulUtils.valueToType(type, request.getHeader(headerName));
            } else if(annotationType == PathVar.class) {
                PathVar pathVar = (PathVar) annotation;
                String name = "{" + pathVar.value() + "}";
                String[] paths = url.split("/");
                for(int i = paths.length - 1; i >= 0; i--) {
                    if(name.equals(paths[i])) {
                        //虽然方法备注为从1开始, 但是这里还是从0开始算
                        return RestfulUtils.valueToType(type, request.getPathParamByIndexL(i));
                    }
                }
            }
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

    public Class<?> getReturnType() {
        return returnType;
    }
}
