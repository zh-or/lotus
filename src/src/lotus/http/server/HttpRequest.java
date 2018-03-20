package lotus.http.server;

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lotus.nio.Session;
import lotus.utils.Utils;

public class HttpRequest {
    private Session                 session         =   null;
    private HttpMethod              mothed          =   null;
    private String                  path            =   null;
    private String                  queryString     =   null;
    private HttpVersion             version         =   null;
    private HashMap<String, String> headers         =   null;
    private byte[]                  body            =   null;
    private Charset                 charset         =   null;
    private HashMap<String, Cookie> cookies         =   null;
    
    public HttpRequest(Session session, Charset charset) {
        headers = new HashMap<String, String>();
        this.session = session;
        this.charset = charset;
    }
    
    public HttpMethod getMothed(){
        return mothed;
    }
    
    public String getPath(){
        return path;
    }
    
    public void setCharacterEncoding(String charset){
        this.charset = Charset.forName(charset);
    }
    
    public Charset getCharacterEncoding(){
        return this.charset;
    }
    
    public void parseHeader(String sheaders){
        final String[] headerFields = sheaders.split("\r\n");
        if(headerFields != null && headerFields.length > 1){
            final String requestLine = headerFields[0];
            for(int i = 1; i< headerFields.length; i++){
                final String [] head = headerFields[i].split(": ");
                if(head != null && head.length >= 2){
                    headers.put(head[0].toLowerCase(), head[1].trim());
                }
            }
            final String[] elements = requestLine.split(" ");
            if(elements != null && elements.length == 3){
                mothed = HttpMethod.valueOf(elements[0]);
                path = elements[1];
                int mid = path.lastIndexOf("?");
                if(mid != -1){
                    queryString = path.substring(mid, path.length());
                    path = path.substring(0, mid);
                }

                if(elements[2].indexOf("HTTP/1.1") != -1){
                    version = HttpVersion.HTTP_1_1;
                }else if(elements[2].indexOf("HTTP/1.0") != -1){
                    version = HttpVersion.HTTP_1_0;
                }
            }
        }
    }
    
    public synchronized Cookie getCookie(String key) {
        Cookie cookie = null;
        if(cookies == null) {
            cookies = new HashMap<String, Cookie>();
            String cookies_str = headers.get("cookie");
            if(!Utils.CheckNull(cookies_str)) {
                String [] cookies_str_arr = cookies_str.split(";");
                for(String item : cookies_str_arr) {
                    item = item.trim();
                    Cookie tmp = Cookie.parseFormString(item);
                    cookies.put(tmp.key, tmp);
                }
            }
        }
        cookie = cookies.get(key);
        return cookie;
    }
    

    
    public void setBody(final byte[] body){
        this.body = body;
    }
    
    public HttpVersion getVersion(){
        return version;
    }
    
    public String getQueryString(){
        return queryString;
    }
    
    /**
     * key已全部转换为小写
     * @param key
     * @return
     */
    public String getHeader(String key){
        return headers.get(key.toLowerCase());
    }
    
    public String getParameter(String name){
        Matcher m = Pattern.compile("[&?]" + name + "=([^&]*)").matcher("&" + queryString);
        if(m.find()){
            return m.group(1);
        }else if(getHeader("Content-Type") != null && getHeader("Content-Type").indexOf("application/x-www-form-urlencoded") != -1){
            m = Pattern.compile("[&]" + name + "=([^&]*)").matcher("&" + new String(body, charset));
            if(m.find()){
                try {
                    return URLDecoder.decode(m.group(1), charset.displayName());
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        return null;
    }
    
    public String getParameter(String name, String defval){
        String val = getParameter(name);
        return val == null ? defval : val;
    }
    
    public byte[] getBody(){
        return this.body;
    }
    
    public SocketAddress getRemoteAddress(){
        return session.getRemoteAddress();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Request [mothed=");
        builder.append(mothed);
        builder.append(", path=");
        builder.append(path);
        builder.append(", queryString=");
        builder.append(queryString);
        builder.append(", version=");
        builder.append(version);
        builder.append(", \nheaders=\n");
        Iterator<Entry<String, String>> it = headers.entrySet().iterator();
        while(it.hasNext()){
        	Entry<String, String> item = it.next();
        	builder.append("\t");
        	builder.append(item.getKey());
        	builder.append(": ");
        	builder.append(item.getValue());
        	builder.append("\n");
        }
        builder.append("body=");
        builder.append(Arrays.toString(body));
        builder.append("\nbodystr=");
        String sbody = "{}";
        if(body != null){
            sbody = new String(body);
        }
        builder.append(sbody);
        builder.append("\n]");
        return builder.toString();
    }
}
