package lotus.test;

import java.util.Calendar;

import lotus.json.JSONException;

public class Test_Util {
    public static void main(String[] args) throws JSONException {
        Calendar cal = Calendar.getInstance();
        //2016 11 27 == 391 and wait 129 day is 520
        //cal.add(Calendar.DAY_OF_YEAR, 129);
        //System.out.println("" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH) + " " + cal.get(Calendar.DAY_OF_MONTH));
        cal.set(2016, 11, 27);
        cal.add(Calendar.DAY_OF_YEAR, -391);
        System.out.println("" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH) + " " + cal.get(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.DAY_OF_YEAR, 520);
        System.out.println("" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH) + " " + cal.get(Calendar.DAY_OF_MONTH));
        
        /*System.out.println(Util.strHash("awa"));
        JSONObject json = new JSONObject();
        JSONArray arr = new JSONArray();
        arr.put(new JSONArray().put(1).put(2));
        arr.put(new JSONArray().put(2).put(2));
        arr.put(new JSONArray().put(3).put(2));
        json.put("arr", arr);
        System.out.println(json);*/
    }
}
