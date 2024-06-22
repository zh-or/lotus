package or.lotus.http;

import or.lotus.common.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XSSFilter {

    public static void main(String[] args) {
        String test = "<script>alert('XSS');</script>\n" +
                "<svg onload=\"alert('XSS')\">\n" +
                "<img  src=\"x\" onerror=\"alert('XSS')\">\n" +
                "<div/style=background-image:url(javascript:alert('XSS'))>\n" +
                "<a href=\"javascript:alert('XSS')\">Link</a>\n" +
                "<input type=text value=<script>alert('XSS')</script>>" +
                "<ScRiPt>AlErt('XSS')</ScRiPt>";

        System.out.println(filterXSSString(test));
    }

    public static final String NULL_STR = "";


    public static String filterXSSString(String value) {
        if(Utils.CheckNull(value)) {
            return NULL_STR;
        }
        String result = replaceAll("<script[^>]*>([^<]*)<\\/script>", value, NULL_STR);
        result = replaceAll("(on[a-z]+)", result, "fuckEvent");
        result = replaceAll("javascript:.*?", result, "fuckEvent");

        return result;
    }

    private static String replaceAll(String pattern, String value, String newString) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(value);
        return m.replaceAll(newString);
    }

}
