package or.lotus.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BeanUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        //反序列化的时候如果多了其他属性,不抛出异常
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //如果是空对象的时候,不抛异常
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        //属性为null不转换
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static <E> E JsonToObj(Class<E> obj, String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, obj);
    }

    public static <E> E JsonToObj(TypeReference<E> obj, String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, obj);
    }

    public static String ObjToJson(Object obj) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    public static JsonNode ObjToJsonNode(Object obj) throws JsonProcessingException {
        return OBJECT_MAPPER.valueToTree(obj);
    }

    public static ObjectNode createNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

    public static JsonNode parseNode(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(json);
    }

    public static ObjectNode parseObject(String json) throws JsonProcessingException {
        return (ObjectNode) parseNode(json);
    }

    public static ArrayNode parseArray(String json) throws JsonProcessingException {
        return (ArrayNode) OBJECT_MAPPER.readTree(json);
    }

    /***
     * 被加载的类必须有无参构造方法
     * @param obj
     * @param classPath
     * @return
     * @param <E>
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public static <E> E loadClassByPath(Class<E> obj, String classPath) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(classPath);
        if(obj.isAssignableFrom(c)) {
            return (E) c.newInstance();
        }
        return null;
    }

    public static List<String> getClassPathByPackage(String packageName) throws URISyntaxException, IOException {

        ArrayList<String> res = new ArrayList<>();
        String pkgPath = packageName.replace(".", "/");
        URL url = Thread.currentThread().getContextClassLoader().getResource(pkgPath);
        String pt = url.getProtocol();

        if ("file".equals(pt)) {
            File dir = new File(url.toURI());
            File[] fs = dir.listFiles();

            for (File f : fs) {
                String fileName = f.getName();
                if (fileName.endsWith(".class")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                    res.add(packageName + "." + fileName);
                }
            }
        } else if ("jar".equals(pt)) {
            try (JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if(!entry.isDirectory()) {
                        String name = entry.getName();
                        if(name.startsWith(pkgPath) && name.endsWith(".class")) {
                            res.add(name.replaceAll("/", ".").substring(0, name.lastIndexOf(".")));
                        }
                    }
                }
            }
        }
        return res;
    }

    public static void copyFields(Object from, Object to) {
        if(from == null || to == null) {
            return ;
        }
        Field[] fields = to.getClass().getDeclaredFields();
        Class<?> clazz = from.getClass();
        for(Field f : fields) {
            try {
                Field ff = clazz.getDeclaredField(f.getName());
                if(ff != null && f.getType() == f.getType()) {
                    ff.setAccessible(true);
                    f.setAccessible(true);
                    f.set(to, ff.get(from));
                }
            } catch (Exception e) {}
        }
    }

}
