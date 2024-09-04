package or.lotus.core.http.restful.support;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.ann.Autowired;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;

public class RestfulUtils {
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
        Field[] fields = clazz.getFields();

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
