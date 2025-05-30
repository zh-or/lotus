package or.lotus.core.test;


import or.lotus.core.common.Utils;
import or.lotus.core.files.FileMap;

public class FilesTest {
    public static void main(String[] args) throws Exception {
        FileMap fm = new FileMap("./test", Utils.formatSize("10GB"), "utf-8");
        fm.put("haha", "haha de content");
        fm.put("haha", "haha re content1222");
        fm.append("haha", "\nappend123");
        System.out.println("返回:" + fm.getString("haha"));
    }
}
