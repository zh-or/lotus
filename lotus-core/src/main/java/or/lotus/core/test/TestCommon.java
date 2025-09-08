package or.lotus.core.test;


import or.lotus.core.common.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCommon {
    public static void main(String[] args) throws Exception {
        UrlMatcher<String> uMatcher = new UrlMatcher<>();

        uMatcher.add("/", "base");
        uMatcher.add("/a/b/c/{userId}", "1");
        uMatcher.add("/a/b/d/2", "2");
        uMatcher.add("/a/b/d/2/a", "2a");
        String[] paths = new String[]{"/", "/a/b/c/1", "/a/b/d/2", "/a/b/d/2/a"};
        int len = paths.length;
        TestTime tt = new TestTime();
        tt.start("url test");
        String p, r;
        for(int i = 0; i < 1000000; i++) {
            p = paths[i % len];
            r = uMatcher.match(paths[i % len]);

            if(r == null) {
                System.out.println(p + ":" + r);
            }
        }
        tt.end();

        tt.start("get");
        UrlMatcher.Node tmp = uMatcher.findNode("/a/b/c/1");
        tmp = uMatcher.findNode("/");
        tmp = uMatcher.findNode("/a/b/d/2");
        tmp = uMatcher.findNode("/a/b/d/2/a");
        for(int i = 0; i < 1000000; i++) {
        }
        tt.end();
        tt.print();

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
