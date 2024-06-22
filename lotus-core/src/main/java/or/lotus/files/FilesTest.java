package or.lotus.files;


import or.lotus.common.Utils;

public class FilesTest {
    public static void main(String[] args) throws Exception {
        FileManager fm = new FileManager("./test", Utils.formatSize("10GB"), "utf-8");
        fm.put("haha", "haha de content");

        System.out.println("返回:" + fm.getString("haha"));
    }
}
