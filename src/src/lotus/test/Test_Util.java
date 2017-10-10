package lotus.test;

import lotus.utils.Utils;

public class Test_Util {
    public static void main(String[] args) throws Exception {
//        Calendar cal = Calendar.getInstance();
        //2016 11 27 == 391 and wait 129 day is 520
        //cal.add(Calendar.DAY_OF_YEAR, 129);
        //System.out.println("" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH) + " " + cal.get(Calendar.DAY_OF_MONTH));
//        cal.set(2016, 11, 27);
//        cal.add(Calendar.DAY_OF_YEAR, -391);
//        System.out.println("" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH) + " " + cal.get(Calendar.DAY_OF_MONTH));
//        cal.add(Calendar.DAY_OF_YEAR, 520);
//        System.out.println("" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH) + " " + cal.get(Calendar.DAY_OF_MONTH));
//        
        /*System.out.println(Util.strHash("awa"));
        JSONObject json = new JSONObject();
        JSONArray arr = new JSONArray();
        arr.put(new JSONArray().put(1).put(2));
        arr.put(new JSONArray().put(2).put(2));
        arr.put(new JSONArray().put(3).put(2));
        json.put("arr", arr);
        System.out.println(json);*/
        
        
//        byte[] a = new byte[]{1,2,3,4,5,6,6,7,8,9,00,65, 12,12,33,4,1,2,5,6,1,8,9};
//        byte[] b = new byte[]{1,8,9};
//        System.out.println(Util.byteArrSearch(a, b));
        
        
//        JSONObject json = new JSONObject();
//        json.put("a", "3");
//        json.put("d", 2);
//        json.put("l", System.currentTimeMillis());
//        json.put("b", false);
//        long start = System.currentTimeMillis();
//        Test t = null;
//        for(int i = 0; i < 10000; i++){
//            t = Util.JsonToObj(Test.class, json);
//        }
//        System.out.println("一万次 json2obj:" + (System.currentTimeMillis() - start));
//        System.out.println(t.toString());
//        JSONObject json2 = null;
//        start = System.currentTimeMillis();
//        for(int i = 0; i < 10000; i++){
//            json2 = Util.ObjToJson(t);
//        }
//        System.out.println("一万次 obj2json:" + (System.currentTimeMillis() - start));
//        System.out.println(json2);
        
        
        String ip = "163.125.241.164";
        long ipint = Utils.ip2int(ip);
        System.out.println(ipint);
        System.out.println(Utils.int2ip(ipint));
    }
    
    
}
