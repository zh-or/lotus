package lotus.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lotus.json.JSONException;
import lotus.json.JSONObject;


/**
 * 不必每个都生成setter, 重写 _set 即可监听
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

	public static <T extends BeanBase> List<T> ResultToBeanList(ResultSet res, Class<T> bean) throws SQLException, Exception{
		List<T> list = new ArrayList<>();
		
		while(res.next()) {
			list.add(ResultToBean(res, bean));
		}
		
		return list;
	}
	
	public static <T extends BeanBase> T ResultToBean(ResultSet res, Class<T> bean) throws Exception {
		T obj = bean.newInstance();
		Field[] fields = bean.getFields();
		String name = null;
		Object val = null;
		for(Field field : fields) {
			name = field.getName();
			//名字需要处理
			val = res.getObject(name);
			obj._set(name, val);
			field.set(obj, val);
		}
		
		return obj;
	}
	
	private static String makeName(String name) {
		StringBuilder sb = new StringBuilder();
		int len = name.length();
		if(len <= 0) {
			return name;
		}
		char c = name.charAt(0);
		if(c >= 65 && c <= 90) {
			c += 32;
		}
		sb.append(c);
		for(int i = 1; i < len; i++) {
			c = name.charAt(i);
			if(c >= 65 && c <= 90) {
				sb.append('_');
				sb.append((char)(c + 32));
			} else {
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
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
                json.put(name, f.get(obj).toString());
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
    
    
    public static void main(String[] args) {
		System.out.println(makeName("AllenPeople"));
	}
}
