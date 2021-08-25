package lotus.test;

import java.util.concurrent.ConcurrentHashMap;


public class TestUtils {
    private static ConcurrentHashMap<String, Long> t = new ConcurrentHashMap<>(100);
    
    
    public static void start(String name) {
        t.put(name, System.currentTimeMillis());
    }
    
    public static void end(String name) {
        long s = t.get(name);
        System.out.println(name + " 用时:" + (System.currentTimeMillis() - s) + "ms");
    }
}
