package or.lotus.v8.support;

import or.lotus.v8.src.*;
import or.lotus.core.common.Utils;
import or.lotus.core.json.JSONArray;
import or.lotus.core.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map;

public class V8T {

    public static String getFileString(String path) throws Exception {
        return getFileString(new File(path));
    }

    /**
     * 读取文件, 默认utf-8编码
     * @param file
     * @return
     * @throws Exception
     */
    public static String getFileString(File file) {
        String script = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            script = new String(bytes, "utf-8" );
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            if(in != null)
                try {
                    in.close();
                } catch (Exception e) {}
        }
        return script;
    }


    public static String formatV8ScriptExecution(Throwable e) {
        if(e == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if(!(e instanceof V8ScriptException)) {
            return Utils.formatException(e);
        }
        V8ScriptException e1 = (V8ScriptException) e;
        sb.append("\n");
        sb.append(e1.getJSMessage()).append("\n");
        sb.append("fileName:").append(e1.getFileName())
            .append("   lineNumber:").append(e1.getLineNumber()).append("\n");
        sb.append("startColumn:").append(e1.getStartColumn())
            .append("    endColumn:").append(e1.getEndColumn()).append("\n");
        sb.append("sourceLine:").append(e1.getSourceLine()).append("\n");
        sb.append(e1.getJSStackTrace());
        return sb.toString();
    }

    /**
     *
     * @param name 可以带后缀也可以不带后缀
     * @param paths
     * @return
     */
    public static File getLibFile(String name, ArrayList<String> paths) {
        if(paths == null) {
            return null;
        }
        int size = paths.size();
        File tmp = null;

        for(int i = 0; i < size; i++){
            String path = name.endsWith(".js") ? name : name + ".js";
            tmp = new File(paths.get(i), path);
            if(tmp.exists()) {
                return tmp;
            }
        }
        return null;
    }

    /**
     * 尽量减少使用此方法, 最好用string给v8传参数
     * @param v8
     * @param obj
     * @return
     * @throws Throwable
     */
    public static V8Object JsonToV8Object(V8 v8, JSONObject obj) throws Throwable {
        if(obj == null) {
            return null;
        }
        V8Object v8Obj = new V8Object(v8);
        Throwable e2 = null;
        try{
            for (Map.Entry<String, Object> entry : obj.entrySet()) {

                String key = entry.getKey();
                Object value = entry.getValue();
                if(value == null) {
                    v8Obj.addNull(key);
                    continue;
                }
                Class<?> type = value.getClass();
                if(type == String.class) {
                    v8Obj.add(key, (String) value);
                } else if(type == Integer.class || type == int.class) {
                    v8Obj.add(key, (int) value);
                } else if(type == Boolean.class || type == boolean.class) {
                    v8Obj.add(key, (boolean) value);
                } else if(type == Double.class || type == double.class) {
                    v8Obj.add(key, (double) value);
                } else if(type == Double.class || type == double.class) {
                    v8Obj.add(key, (double) value);
                } else if(type == JSONArray.class) {
                    V8Array arr = JsonToV8Array(v8, (JSONArray) value);
                    v8Obj.add(key, arr);
                    arr.close();
                } else if(type == JSONObject.class) {
                    V8Object _obj = JsonToV8Object(v8, (JSONObject) value);
                    v8Obj.add(key, _obj);
                    _obj.close();
                } else {
                    //忽略的类型
                }
            }
        } catch(Throwable e) {
            e2 = e;
        }
        if(e2 != null){
            v8Obj.close();
            v8Obj = null;
            throw e2;
        }
        return v8Obj;
    }

    /**
     * 尽量减少使用此方法, 最好用string给v8传参数
     * @param v8
     * @param arr
     * @return
     * @throws Throwable
     */
    public static V8Array JsonToV8Array(V8 v8, JSONArray arr) throws Throwable{
        if(arr == null) {
            return null;
        }
        V8Array v8Arr = new V8Array(v8);
        Throwable e2 = null;
        try{
            int len = arr.length();
            for(int i = 0; i < len; i ++) {
                Object obj = arr.get(i);
                Class<?> type = obj.getClass();
                if(type == String.class) {
                    v8Arr.push((String) obj);
                } else if(type == Integer.class || type == int.class) {
                    v8Arr.push((int) obj);
                } else if(type == Boolean.class || type == boolean.class) {
                    v8Arr.push((boolean) obj);
                } else if(type == Double.class || type == double.class) {
                    v8Arr.push((double) obj);
                } else if(type == Double.class || type == double.class) {
                    v8Arr.push((double) obj);
                } else if(type == JSONArray.class) {
                    V8Array _arr = JsonToV8Array(v8, (JSONArray) obj);
                    v8Arr.push(_arr);
                } else if(type == JSONObject.class) {
                    V8Object _obj = JsonToV8Object(v8, (JSONObject) obj);
                    v8Arr.push(_obj);
                    _obj.close();
                } else {
                    //忽略的类型
                }

            }
        } catch(Throwable e) {
            e2 = e;
        }
        if(e2 != null) {
            v8Arr.close();
            v8Arr = null;
            throw e2;
        }
        return v8Arr;
    }

    public static JSONObject V8ObjectToJson(V8Object obj) throws Throwable {
        if(obj == null) {
            return null;
        }
        JSONObject jsonObj = new JSONObject();
        String[] keys = obj.getKeys();
        Throwable e2 = null;
        V8Object value = null;
        try{
            for(String key : keys) {
                int type = obj.getType(key);
                switch(type) {
                    case V8Value.INTEGER:
                    case V8Value.DOUBLE:
                    case V8Value.BYTE:
                        jsonObj.put(key, obj.getInteger(key));
                        break;
                    case V8Value.STRING:
                        jsonObj.put(key, obj.getString(key));
                        break;
                    case V8Value.BOOLEAN:
                        jsonObj.put(key, obj.getBoolean(key));
                        break;
                    case V8Value.V8_OBJECT:
                        value = obj.getObject(key);
                        jsonObj.put(key, V8ObjectToJson(value));
                        value.close();
                        break;
                    case V8Value.V8_ARRAY:
                        value = obj.getArray(key);
                        jsonObj.put(key, V8ArrayToJson((V8Array) value));
                        value.close();
                        break;
                    case V8Value.NULL:
                    case V8Value.UNDEFINED:
                        jsonObj.put(key, JSONObject.NULL);
                        break;
                }
            }
        } catch(Throwable e) {
            e2 = e;
        }
        if(e2 != null) {
            if(value != null){
                value.close();
                value = null;
            }
            throw e2;
        }

        return jsonObj;
    }

    public static JSONArray V8ArrayToJson(V8Array arr) throws Throwable {
        if(arr == null) {
            return null;
        }
        JSONArray jsonArr = new JSONArray();
        int len = arr.length();
        Throwable e2 = null;
        Object value = null;
        try{
            for(int i = 0; i < len; i ++) {
                int type = arr.getType(i);
                value = arr.get(i);
                switch(type) {
                    case V8Value.INTEGER:
                    case V8Value.DOUBLE:
                    case V8Value.BYTE:
                        jsonArr.put((Integer) value);
                        break;
                    case V8Value.STRING:
                        jsonArr.put((String) value);
                        break;
                    case V8Value.BOOLEAN:
                        jsonArr.put((Boolean) value);
                        break;
                    case V8Value.V8_OBJECT:
                        jsonArr.put(V8ObjectToJson((V8Object) value));
                        ((V8Value) value).close();
                        break;
                    case V8Value.V8_ARRAY:
                        jsonArr.put(V8ArrayToJson((V8Array) value));
                        ((V8Value) value).close();
                        break;
                    case V8Value.NULL:
                    case V8Value.UNDEFINED:
                        jsonArr.put(JSONObject.NULL);
                        break;
                }
            }
        } catch(Throwable e) {
            e2 = e;
        }
        if(e2 != null) {
            if(value != null && value instanceof  V8Value) {
                ((V8Value) value).close();
                value = null;
            }
            throw e2;
        }

        return jsonArr;
    }


}
