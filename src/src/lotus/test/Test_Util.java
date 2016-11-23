package lotus.test;

import lotus.json.JSONArray;
import lotus.json.JSONException;
import lotus.json.JSONObject;
import lotus.util.Util;

public class Test_Util {
    public static void main(String[] args) throws JSONException {
        System.out.println(Util.strHash("awa"));
        JSONObject json = new JSONObject();
        JSONArray arr = new JSONArray();
        arr.put(new JSONArray().put(1).put(2));
        arr.put(new JSONArray().put(2).put(2));
        arr.put(new JSONArray().put(3).put(2));
        json.put("arr", arr);
        System.out.println(json);
    }
}
