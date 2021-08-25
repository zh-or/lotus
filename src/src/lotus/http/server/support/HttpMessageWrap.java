package lotus.http.server.support;


public class HttpMessageWrap {
    public static final int HTTP_MESSAGE_TYPE_HEADER = 0;
    public static final int HTTP_MESSAGE_TYPE_BUFFER = 1;
    public static final int HTTP_MESSAGE_TYPE_FILE   = 2;
    
    
    public int type;
    public Object data;

    public HttpMessageWrap(int type, Object data) {
        this.type = type;
        this.data = data;
    }
}
