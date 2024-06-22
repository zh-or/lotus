package or.lotus.test;


import or.lotus.common.Address;
import or.lotus.common.Utils;

import java.util.ArrayList;
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


        ArrayList<Address> address = Utils.getNetworkInfo(true);

        System.out.println(address.toString());
    }
}
