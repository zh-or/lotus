package or.lotus.core.http.restful.support;

import com.fasterxml.jackson.databind.JsonNode;
import or.lotus.core.common.Utils;
import or.lotus.core.http.RestfulApiException;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.ann.Autowired;
import or.lotus.core.http.restful.ann.Parameter;
import or.lotus.core.http.restful.ann.Prop;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.sql.Time;
import java.util.Date;
import java.util.List;

public class RestfulUtils {

    /** 如果t为true则抛出RestfulApiException异常 */
    public static void assets(boolean t, String msg) {
        if(t) {
            throw new RestfulApiException(msg);
        }
    }

    /** 如果对象为空则抛出RestfulApiException异常 */
    public static void assets(Object obj, String msg) {
        if(obj == null) {
            throw new RestfulApiException(msg);
        }
    }


    /** 如果字符串为null或长度为0则抛出RestfulApiException异常 */
    public static void assets(String str, String msg) {
        if(Utils.CheckNull(str)) {
            throw new RestfulApiException(msg);
        }
    }

    public static Object[] valueToArray(Class type, Object[] value) {
        Utils.assets(value == null, "value 为空");
        Utils.assets(type == null, "type 为空");

        Object[] res = (Object[]) Array.newInstance(type, value.length);
        for(int i = 0; i < value.length; i++) {
            res[i] = valueToType(type, value[i]);
        }
        return res;
    }

    public static Object valueToType(Class type, Object value) {
        if(value == null) {
            return null;
        }
        try {

            if (String.class == type) {
                return value.toString();
            } else if (boolean.class == type || Boolean.class == type) {
                return Boolean.parseBoolean(value.toString());
            }  else if (java.sql.Date.class == type) {
                return new Date(value.toString());
            } else if (java.sql.Time.class == type) {
                return Time.valueOf(value.toString());
            } /*else if (java.sql.Timestamp.class == type || java.util.Date.class == type) {
            return rs.getTimestamp(index);
        } */else if (type.isEnum()) {
                return Enum.valueOf(type, value.toString());
            } else {
                String val = value.toString();
                if(val.length() < 1) {
                    return null;
                }
                if (byte.class == type || Byte.class == type) {
                    return Byte.valueOf(val);
                } else if (short.class == type || Short.class == type) {

                    return Short.valueOf(val);
                } else if (int.class == type || Integer.class == type) {
                    return Integer.valueOf(val);
                } else if (long.class == type || Long.class == type) {
                    return Long.valueOf(val);
                } else if (float.class == type || Float.class == type) {
                    return Float.valueOf(val);
                } else if (double.class == type || Double.class == type) {
                    return Double.valueOf(val);
                } else if (BigDecimal.class == type) {
                    return new BigDecimal(val);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            //类型转换失败直接返回null
        }

        return null;
    }

    public static Object jsonValueToType(Class type, JsonNode value) {
        if(value.isMissingNode() || value.size() < 1) {
            return null;
        }
        if (String.class == type) {
            return value.asText();
        } else if (boolean.class == type || Boolean.class == type) {
            return value.asBoolean();
        } else if (byte.class == type || Byte.class == type) {
            return Byte.valueOf(value.asText());
        } else if (short.class == type || Short.class == type) {
            return Short.valueOf((short) value.asInt());
        } else if (int.class == type || Integer.class == type) {
            return value.asInt();
        } else if (long.class == type || Long.class == type) {
            return value.asLong();
        } else if (float.class == type || Float.class == type) {
            return (float) value.asDouble();
        } else if (double.class == type || Double.class == type) {
            return value.asDouble();
        } else if (BigDecimal.class == type) {
            return value.decimalValue();
        } else if(BigInteger.class == type) {
            return value.bigIntegerValue();
        } else if (java.sql.Date.class == type) {
            return new Date(value.toString());
        } else if (java.sql.Time.class == type) {
            return Time.valueOf(value.toString());
        } /*else if (java.sql.Timestamp.class == type || java.util.Date.class == type) {
            return rs.getTimestamp(index);
        } */else if (type.isEnum()) {
            return Enum.valueOf(type, value.toString());
        }

        return null;
    }

    public static boolean isBaseType(Class clazz) {
        return clazz == int.class
                || clazz == long.class
                || clazz == float.class
                || clazz == double.class
                || clazz == boolean.class
                || clazz == byte.class
                || clazz == char.class
                || clazz == short.class
                || clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == Boolean.class
                || clazz == Byte.class
                || clazz == Character.class
                || clazz == Short.class ;
    }


    public static String getMimeType(String charset, File fileUrl) {

        //String type = URLConnection.guessContentTypeFromName(fileUrl.getPath());
        String type = null;
        try {
            type = Files.probeContentType(fileUrl.toPath());
        } catch (IOException e) {
        }
        if(type == null) {
            if(fileUrl.toPath().endsWith(".js")) {
                return "text/javascript; charset=" + charset;
            }
            return "text/html; charset=" + charset;
        }
        return type;
    }

    public static Object getPropObject(RestfulContext context, Class<?> type, Prop prop) {
        String name = prop.value();
        if(!Utils.CheckNull(name)) {
            if(type.isArray()) {
                return  RestfulUtils.valueToArray(
                        type.getComponentType(),
                        context.getStringArrayConfig(name, prop.def())
                );
            } else if(type.isAssignableFrom(List.class)) {
                throw new RuntimeException("List 类型的属性暂不支持, 请使用数组 " + prop.value());
            } else {
                String ini;
                if(prop.random()) {
                    ini = context.getRandomStringConfig(name, prop.def());
                } else {
                    ini = context.getStringConfig(name, prop.def());
                }
                return RestfulUtils.valueToType(type, ini);
            }
        }
        return null;
    }

    public static Object injectPropAndInvokeMethod(RestfulContext context, Object obj, Method method) throws IllegalAccessException, InvocationTargetException {
        Class[] parameterTypes = method.getParameterTypes();
        int size = parameterTypes.length;
        Object[] parameters = new Object[size];

        for(int i = 0; i < size; i ++) {
            Class type = parameterTypes[i];
            Annotation[][] anns = method.getParameterAnnotations();
            parameters[i] = null;
            if(type.isInstance(context)) {
                parameters[i] = context;
            } else if(anns[i].length > 0) {
                for(Annotation a : anns[i]) {
                    Class annType = a.annotationType();
                    if(annType == Prop.class) {
                        Prop prop = (Prop) a;
                        parameters[i] = getPropObject(context, type, prop);
                        break;
                    }
                    if (annType == Autowired.class) {
                        Autowired autowired = (Autowired) a;
                        String name = autowired.value();
                        if(Utils.CheckNull(name)) {
                            //使用全限定名
                            name = type.getName();
                        }
                        parameters[i] = context.getBean(name);
                        break;
                    }
                }
            }
        }

        return method.invoke(obj, parameters);
    }

    /** 注入Bean到对象 */
    public static void injectBeansToObject(RestfulContext context, Object obj) throws IllegalAccessException {
        Class clazz = obj.getClass();

        Field[] fields = clazz.getDeclaredFields();

        for(Field field : fields) {

            Autowired autowired = field.getAnnotation(Autowired.class);
            if(autowired != null) {
                String name = autowired.value();
                if(Utils.CheckNull(name)) {
                    //使用全限定名
                    name = field.getType().getName();
                }

                Object bean = context.getBean(name);
                if(bean != null) {
                    field.setAccessible(true);
                    field.set(obj, bean);
                } else {
                    throw new RuntimeException(String.format(
                            "%s 注入 %s 时 %s 还未注册, 请调整 Bean 的 order ",
                            clazz.getName(),
                            name,
                            name
                    ));
                }
            }

            Prop prop = field.getAnnotation(Prop.class);
            if(prop != null) {
                field.setAccessible(true);
                field.set(obj, getPropObject(context, field.getType(), prop));
            }

        }
    }
}
