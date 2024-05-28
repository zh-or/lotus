package or.lotus.json;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;


/**
 * 不必每个都生成setter, 重写 _set 即可监听
 * 此类不能为内部类, 必须要有一个空的构造方法
 * @author or
 *
 */
public class BeanBase {


    public BeanBase() {

    }

	/**
	 *
	 * @param key
	 * @param val
	 */
	public  void _set(String key, Object val) {}

	public JSONObject toJSON() throws Exception {
	    return BeanBase.ObjToJson(this);
	}


    /**
     *
     * @param obj
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
            String name = f.getName();
            if(!json.has(name)) {
                continue;
            }
            f.setAccessible(true);
            Class<?> type = f.getType();
            if (type == String.class)
                f.set(e, json.getString(name));
            else if (type == boolean.class || type == Boolean.class)
                f.setBoolean(e, json.getBoolean(name));
            else if (type == long.class || type == Long.class)
                f.set(e, json.getLong(name));
            else if (type == int.class || type == char.class || type == byte.class || type == short.class ||
                    type == Integer.class || type == Byte.class || type == Short.class
            )
                f.set(e, json.getInt(name));
            else if (type == float.class || type == double.class ||
                    type == Float.class || type == Double.class
            )
                f.set(e, json.getDouble(name));
            else {
                f.set(e, JsonToObj(type, json.getJSONObject(name)));
            }
        }
        return e;
    }

    /**
     * 对象转json
     * @param obj 如果有字段也是对象需要转换, 那么此字段的类型必须是继承自 BeanBase
     * @return
     * @throws Exception
     */
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
            else if (type == boolean.class || type == Boolean.class)
                json.put(name, f.getBoolean(obj));
            else if (type == long.class || type == Long.class)
                json.put(name, (Long) f.get(obj));
            else if (type == int.class || type == Integer.class)
                json.put(name, (Integer) f.get(obj));
            else if (type == char.class)
                json.put(name, f.getChar(obj));
            else if (type == byte.class || type == Byte.class)
                json.put(name, f.getByte(obj));
            else if (type == short.class || type == Short.class)
                json.put(name, f.getShort(obj));
            else if (type == float.class || type == Float.class)
                json.put(name, f.getFloat(obj));
            else if (type == double.class || type == Double.class)
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


/*    public static <E> E SQLResToObj(Class<E> obj) {
        Field[] fields = obj.getDeclaredFields();


        return null;
    }*/
}
