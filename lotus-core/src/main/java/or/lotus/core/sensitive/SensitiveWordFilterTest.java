package or.lotus.core.sensitive;

import java.io.File;

public class SensitiveWordFilterTest {
    public static void main(String[] args) throws Exception {
        SensitiveWordFilter sensitiveWordFilter = new SensitiveWordFilter();
        sensitiveWordFilter.load(new File("./SensitiveWordList.txt"));
        StringBuilder stringBuilder = new StringBuilder();
        long t1, t2;
        for (int i = 0; i < 100; i++) {
            stringBuilder.append("123TM,D123");
        }
        String s = stringBuilder.toString();
        WordFilterResult result = null;
        t1 = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            result = sensitiveWordFilter.filter(s);
            int a = 1 + 41;
        }
        t2 = System.nanoTime();
        System.out.println(result);
        System.out.println((t2 - t1) / 1000000 + "毫秒");
    }
}
