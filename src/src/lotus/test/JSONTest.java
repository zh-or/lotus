package lotus.test;

import lotus.json.JSONArray;
import lotus.json.JSONException;
import lotus.json.JSONObject;

public class JSONTest {
    public static void main(String[] args) throws JSONException {
         JSONArray arr = new JSONArray();
         for(int i = 0; i < 10; i ++){
             arr.put(i << 1);
         }
         
         for(int i = 0; i < arr.length(); i++) {
             Object obj = arr.get(i);
             System.out.println(obj.toString());
         }
         
    }
}
