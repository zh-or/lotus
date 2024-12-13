package or.lotus.core.test;


import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.NetWorkAddress;
import or.lotus.core.common.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCommon {
    public static void main(String[] args) throws Exception {
        int[] a = Utils.getNumberFromStr("1,------2-,3,4,5,6,7,8aaa9dd,10", true);


        List<String> clazz = BeanUtils.getClassPathByPackage("or.lotus.core.http.*");


        long v = 256866551110225l;
        String x = Utils.longTo35(v);

        long v2 = Utils.str35ToLong(x);

        System.out.println("->" + (v == v2) + " :" +  x);

        String input = "/a/b/c/{userId}";
        String regex = "/\\{(.*)\\}";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String userId = matcher.group(1);
            System.out.println(userId);
        } else {
            System.out.println("No match found.");
        }


        ArrayList<NetWorkAddress> netWorkAddresses = Utils.getNetworkInfo(true);

        System.out.println(netWorkAddresses.toString());

        Method[] ms = TestCommon.class.getMethods();
        for(Method m : ms) {
            if("test".equals(m.getName()) ) {
                Object[] arg = new Object[]{
                        "a", "啊,b,v".split(",")
                };


                m.invoke(new TestCommon(), arg);
            }
        }
    }

    public  void test(String a, String[] b) {
        System.out.println("a:" + a + ":" + Arrays.toString(b));
    }
}
