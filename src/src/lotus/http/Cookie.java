package lotus.http;

public class Cookie {
    public String path;
    public String key;
    public String value;
    public long time;
    
    
    public Cookie(String key, String value) {
        this.path = "/";
        this.key = key;
        this.value = value;
    }



    public Cookie(String path, String key, String value) {
        this.path = path;
        this.key = key;
        this.value = value;
    }

    public Cookie(String path, String key, String value, long time) {
        this.path = path;
        this.key = key;
        this.value = value;
        this.time = time;
    }


    @Override
    public String toString() {
        return "Cookie [path=" + path + ", key=" + key + ", value=" + value + ", time=" + time + "]";
    }
    
    
    public static Cookie parseFormString(String str) {
        Cookie cookie = null;
        int w = str.indexOf("=");
        if(w != -1) {
            String key = str.substring(0, w);
            String val = str.substring(w + 1, str.length());
            cookie = new Cookie(key, val);
        }
        return cookie;
    }
    
    public String toRaw() {
        return key + "=" + value;
    }
}
