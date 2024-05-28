package or.lotus.common.test;


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
    }
}
