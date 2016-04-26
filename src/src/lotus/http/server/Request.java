package lotus.http.server;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lotus.nio.Session;

public class Request {
    private Session                 session         =   null;
    private HttpMethod              mothed          =   null;
    private String                  path            =   null;
    private String                  queryString     =   null;
    private HttpVersion             version         =   null;
    private HashMap<String, String> headers         =   null;
    private byte[]                  body            =   null;
    
    public Request(Session session) {
        headers = new HashMap<String, String>();
        this.session = session;
    }
    
    public HttpMethod getMothed(){
        return mothed;
    }
    
    public void parseHeader(String sheaders){
        final String[] headerFields = sheaders.split("\r\n");
        if(headerFields != null && headerFields.length > 1){
            final String requestLine = headerFields[0];
            for(int i = 1; i< headerFields.length; i++){
                final String [] head = headerFields[i].split(":");
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

    
    public void setBody(final byte[] body){
        this.body = body;
    }
    
    public HttpVersion getVersion(){
        return version;
    }
    
    public String getQueryString(){
        return queryString;
    }
    
    public String getHeader(String key){
        return headers.get(key);
    }
    
    public String getParameter(String name){
        Matcher m = Pattern.compile("[&]"+name+"=([^&]*)").matcher("&"+queryString);
        if(m.find()){
            return m.group(1);
        }
        return null;
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
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", body=");
        builder.append(Arrays.toString(body));
        builder.append("]");
        return builder.toString();
    }
}
