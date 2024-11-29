package or.lotus.core.http.restful.support;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class RestfulCookie {
    public String path;
    public String key;
    public String value;
    public long time;


    public RestfulCookie(String key, String value) {
        this.path = "/";
        this.key = key;
        this.value = value;
    }



    public RestfulCookie(String path, String key, String value) {
        this.path = path;
        this.key = key;
        this.value = value;
    }

    public RestfulCookie(String path, String key, String value, long time) {
        this.path = path;
        this.key = key;
        this.value = value;
        this.time = time;
    }


    @Override
    public String toString() {
        return "Cookie [path=" + path + ", key=" + key + ", value=" + value + ", time=" + time + "]";
    }


    public static RestfulCookie parseFormString(String str, String charset) {
        RestfulCookie cookie = null;
        int w = str.indexOf("=");
        if(w != -1) {
            String key = str.substring(0, w);
            String val = str.substring(w + 1, str.length());
            try {
                cookie = new RestfulCookie(key, URLDecoder.decode(val, charset));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return cookie;
    }

    public String toRaw() {
        return key + "=" + value;
    }
}
