package lotus.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import lotus.json.JSONException;
import lotus.json.JSONObject;


/**
 * 不必每个都生成setter, 重写 _set 即可监听
 * 此类不能为内部类, 必须要有一个空的构造方法
 * @author or
 *
 */
public class BeanBase {
	
	/**
	 * 
	 * @param key
	 * @param val
	 */
	public  void _set(String key, Object val) {}


	/**
     * 
     * @param c
     *            此类不能为内部类, 必须要有一个空的构造方法
     * @param json
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws JSONException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static <E> E JsonToObj(Class<E> obj, JSONObject json) throws Exception {
        E e = obj.newInstance();
        Field[] fields = obj.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            Class<?> type = f.getType();
            String name = f.getName();
            if (type == String.class)
                f.set(e, json.getString(name));
            else if (type == boolean.class)
                f.setBoolean(e, json.getBoolean(name));
            else if (type == long.class)
                f.setLong(e, json.getLong(name));
            else if (type == int.class || type == char.class || type == byte.class || type == short.class)
                f.set(e, json.getInt(name));
            else if (type == float.class || type == double.class)
                f.set(e, json.getDouble(name));
        }
        return e;
    }

    public static JSONObject ObjToJson(Object obj) throws Exception {
        JSONObject json = new JSONObject();
        Class<?> cla = obj.getClass();
        Field[] fields = cla.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            Class<?> type = f.getType();
            String name = f.getName();
            if (type == String.class)
                json.put(name, f.get(obj));
            else if (type == boolean.class)
                json.put(name, f.getBoolean(obj));
            else if (type == long.class)
                json.put(name, f.getLong(obj));
            else if (type == int.class)
                json.put(name, f.getInt(obj));
            else if (type == char.class)
                json.put(name, f.getChar(obj));
            else if (type == byte.class)
                json.put(name, f.getByte(obj));
            else if (type == short.class)
                json.put(name, f.getShort(obj));
            else if (type == float.class)
                json.put(name, f.getFloat(obj));
            else if (type == double.class)
                json.put(name, f.getDouble(obj));
            else {
            	Object child = f.get(obj);
            	if(child != null && BeanBase.class.isAssignableFrom(type)) {
            		json.put(name, ObjToJson(child));
            	} else {
                    json.put(name, child);
            	}
            }
        }
        return json;
    }

    public static boolean ObjSet(Object obj, String name, Object val) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.set(obj, val);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static Object ObjGet(Object obj, String name) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
        }
        return null;
    }
    
}
