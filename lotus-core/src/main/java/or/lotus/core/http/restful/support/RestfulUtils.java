package or.lotus.core.http.restful.support;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.ann.Autowired;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Time;
import java.util.Date;

public class RestfulUtils {
    public static Object[] valueToArray(Class type, Object[] value) {
        Utils.assets(value == null, "value 为空");
        Utils.assets(type == null, "type 为空");

        Object[] res = new Object[value.length];
        for(int i = 0; i < value.length; i++) {
            res[i] = valueToType(type, value[i]);
        }
        return res;
    }

    public static Object valueToType(Class type, Object value) {
        if (String.class == type) {
            return value.toString();
        } else if (boolean.class == type || Boolean.class == type) {
            return Boolean.parseBoolean(value.toString());
        } else if (byte.class == type || Byte.class == type) {
            return Byte.valueOf(value.toString());
        } else if (short.class == type || Short.class == type) {
            return Short.valueOf(value.toString());
        } else if (int.class == type || Integer.class == type) {
            return Integer.valueOf(value.toString());
        } else if (long.class == type || Long.class == type) {
            return Long.valueOf(value.toString());
        } else if (float.class == type || Float.class == type) {
            return Float.valueOf(value.toString());
        } else if (double.class == type || Double.class == type) {
            return Double.valueOf(value.toString());
        } else if (BigDecimal.class == type) {
            return new BigDecimal(value.toString());
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
                    name = field.getDeclaringClass().getName();
                }

                Object bean = context.getBean(name);
                if(bean != null) {
                    field.setAccessible(true);
                    field.set(obj, bean);
                } else {
                    throw new RuntimeException(String.format(
                            "%s 注入 %s 时 %s 还未注册, 请检查是否在 addBeans 方法之前调用了 scanController ",
                            clazz.getName(),
                            name,
                            name
                    ));
                }
            }
        }
    }
}
