package lotus.http.server.support;

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

import lotus.http.server.HttpServer;
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
    private boolean                 isWebSocket     =   false;
    private HttpServer              context         =   null;
    private HttpFormData            formData        =   null;
    private HashMap<String, Object> attrs           =   null;
    
    public HttpRequest(Session session, Charset charset, HttpServer context) {
        headers = new HashMap<String, String>();
        attrs = new HashMap<String, Object>();
        this.session = session;
        this.charset = charset;
        this.context = context;
    }
    
    public Object getAttr(String key, Object defval){
        Object val = attrs.get(key);
        if(val == null) return defval;
        return val;
    }

    public Object getAttr(String key){
        return getAttr(key, null);
    }
    
    public void setAttr(String key, Object val){
        attrs.put(key, val);
    }
    
    public Object removeAttr(String key){
        return attrs.remove(key);
    }
    
    public boolean isFormData() {
        return this.formData != null;
    }
    
    public void setFormData(HttpFormData formData) {
        this.formData = formData;
    }
    
    public HttpFormData getFormData() {
        return formData;
    }
    
    public HttpServer getContext(){
        return context;
    }
    
    public HttpMethod getMothed(){
        return mothed;
    }
    
    public String getPath(){
        if(Utils.CheckNull(path)) {
            return "/";
        }
        return path;
    }
    
    public String getFullPath(){
        if(queryString != null){
            return path + queryString;
        }
        return path;
    }
    
    public boolean isWebSocketConnection() {
        return isWebSocket;
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
            
            if(context.isOpenWebSocket()){
                String connection = getHeader("Connection");
                if(
                        "Upgrade".equals(connection) || 
                        (!Utils.CheckNull(connection) && connection.indexOf("Upgrade") != -1)
                         /*这里可能是多个值, 如 FireFox-> Connection:keep-alive, Upgrade*/
                  ) {
                    String Upgrade = getHeader("Upgrade").toLowerCase();
                    if("websocket".equals(Upgrade)) {//websocket 协议
                        isWebSocket = true;
                    }
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
    
    public String getParameter(String name) {
        Matcher m = Pattern.compile("[&?]" + name + "=([^&]*)").matcher("&" + queryString);
        if(m.find()){
            String par = m.group(1);
            try {
                return URLDecoder.decode(par, charset.displayName());
            } catch (Exception e) {
                // TODO: handle exception
            }
        }else if(body != null && getHeader("Content-Type") != null && getHeader("Content-Type").indexOf("application/x-www-form-urlencoded") != -1){
            String bodyStr = new String(body, charset);
            
            m = Pattern.compile("[&]" + name + "=([^&]*)").matcher("&" + bodyStr);
            if(m.find()){
                try {
                    String par = m.group(1);
                    return URLDecoder.decode(par, charset.displayName());
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
    
    /**
     * 检查是否包含参数key, 只能用于检查get方法, post的 url编码方法
     * @param pars 参数key数组
     * @return 如果有为空的则返回 false
     */
    public boolean checkparameter(String[] pars){
        if(pars == null || pars.length <= 0) return true;
        for(int i = 0; i < pars.length; i++){
            if(getParameter(pars[i]) == null) {
                return false;
            }
        }
        return true;
    }
    
    public byte[] getBody(){
        return this.body;
    }
    
    public SocketAddress getRemoteAddress(){
        return session.getRemoteAddress();
    }
    
    public String getIpAddress() {  
        
        String ip = getHeader("X-Forwarded-For");  

        try {
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
                    ip = getHeader("Proxy-Client-IP");
                }  
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
                    ip = getHeader("WL-Proxy-Client-IP");
                }  
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
                    ip = getHeader("HTTP_CLIENT_IP");
                }  
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
                    ip = getHeader("HTTP_X_FORWARDED_FOR"); 
                }  
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
                    ip = getRemoteAddress().toString();
                }  
            } else if (ip.length() > 15) {  
                String[] ips = ip.split(",");  
                for (int index = 0; index < ips.length; index++) {  
                    String strIp = (String) ips[index];  
                    if (!("unknown".equalsIgnoreCase(strIp))) {  
                        ip = strIp;  
                        break;  
                    }  
                }  
            }
            int p = ip.indexOf(":");
            if(p != -1){
                ip = ip.substring(0, p);
            }
        } catch (Exception e) {
        }
        return ip;  
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
