package or.lotus.core.test;


import or.lotus.core.common.NetWorkAddress;
import or.lotus.core.common.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCommon {
    public static void main(String[] args) throws Exception {
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
