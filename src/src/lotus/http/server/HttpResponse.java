package lotus.http.server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import lotus.json.JSONObject;
import lotus.nio.Session;
import lotus.utils.Base64;
import lotus.utils.Utils;


public class HttpResponse {
	private static final int write_buffer_size		=	2048;
	
	private ByteBuffer					buff                =   null;
    private Session                     session             =   null;
    private ResponseStatus              status              =   null;
    private HashMap<String, String>     headers             =   null;
    private boolean				        isSendHeader        =   false;
    private boolean                     isOpenSync          =   false;
    private Charset                     charset             =   null;
    
    public static HttpResponse defaultResponse(Session session, HttpRequest request){
    	HttpResponse response = new HttpResponse(session, request.isWebSocketConnection() ? ResponseStatus.INFORMATIONAL_SWITCHING_PROTOCOLS : ResponseStatus.SUCCESS_OK);
    	response.setHeader("Server", "simple http server by lotus");
    	Date time = new Date();
    	response.setHeader("Expires", time.toString());
    	response.setHeader("Date", time.toString());
    	String connection = request.getHeader("connection");
    	if(connection != null){
    		response.setHeader("Connection", connection);
    	}
        response.setCharacterEncoding(request.getCharacterEncoding());
        
        if(request.isWebSocketConnection() && request.getContext().isOpenWebSocket()) {
            response.setHeader("Upgrade", request.getHeader("Upgrade"));
            response.setHeader("Connection", "Upgrade");
            String sec = request.getHeader("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            try {
                sec = Base64.byteArrayToBase64(Utils.SHA1(sec));
            }catch(Exception e) {
                sec = "";
            }
            
            response.setHeader("Sec-WebSocket-Accept", sec);
            //response.setHeader("Sec-WebSocket-Protocol", "");
        }
    	return response;
    }
    
    
    
    public HttpResponse(Session session) {
       this(session, ResponseStatus.SUCCESS_OK);
    }

    public HttpResponse(Session session, ResponseStatus status) {
        this.session = session;
        this.status = status;
        this.headers = new HashMap<String, String>();
        this.isSendHeader = false;
        this.buff = ByteBuffer.allocate(write_buffer_size);
        this.headers.put("Content-Type", "text/html");
    }
    
    public void setCharacterEncoding(Charset charset){
        this.charset = charset;
    }
    
    public void setStatus(ResponseStatus status){
        this.status = status;
    }
    
    /**
     * 发送302跳转
     * @param path
     */
    public void sendRedirect(String path){
        this.status = ResponseStatus.REDIRECTION_FOUND;
        headers.put("Location", path);
    //    headers.put("Connection", "close");
        sendHeader();
    }
    
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }
    
    public HttpResponse openSync(){
        isOpenSync = true;
    	//headers.put("Content-Encoding", "gzip");
    	headers.put("Transfer-Encoding", "chunked");
    	headers.remove("Content-Length");
    	sendHeader();
    	return this;
    }
    
    public boolean isOpenSync() {
        return isOpenSync;
    }
    
    public HttpResponse setHeader(String key, String value){
    	headers.put(key, value);
    	return this;
    }
    
    public HttpResponse removeHeader(String key){
        headers.remove(key);
        return this;
    }
    
    public HttpResponse write(JSONObject json){
        setHeader("Content-Type", "application/json");
        write(json.toString());
        return this;
    }
    
    public HttpResponse write(String str){
    	byte[] data = str.getBytes(charset);
    	write(data);
    	return this;
    }
    
    public HttpResponse write(byte[] b){
        int len = b.length;
        String hexlen = Integer.toHexString(len);
        if(isOpenSync) {
            len += 4;
            len += hexlen.length();
        }
    	if(buff.remaining() < b.length){/*需要扩容*/
    		byte[] data = Arrays.copyOf(buff.array(), buff.limit() + b.length);
    		buff = ByteBuffer.wrap(data);
    	}
    	if(isOpenSync) {
    	    buff.put(hexlen.getBytes());
    	    buff.put("\r\n".getBytes());
    	}
    	buff.put(b);
    	if(isOpenSync) {
            buff.put("\r\n".getBytes());
    	}
    	return this;
    }
    
    public void syncEnd() {
        isOpenSync = false;
        write("0\r\n\r\n");
        flush();
    }
    
    private HttpResponse sendHeader(){
    	if(isSendHeader) return this;
    	StringBuilder sb = new StringBuilder();
    	sb.append(status.line());
    	Iterator<Entry<String, String>> it = headers.entrySet().iterator();
    	while(it.hasNext()){
    		Entry<String, String> item = it.next();
    		sb.append(item.getKey());
    		sb.append(": ");
    		sb.append(item.getValue());
    		sb.append("\r\n");
    	}
    	sb.append("\r\n");
    	
    	ByteBuffer buffer = ByteBuffer.allocate(sb.length());
    	buffer.put(sb.toString().getBytes());
    	buffer.flip();
    	session.write(buffer);
    	isSendHeader = true;
    	return this;
    }
    
    public HttpResponse flush(){
    	if(!isSendHeader){
    		setHeader("Content-Length", buff.position() + "");
    		sendHeader();
    	}
    	if(buff.limit() > 0){
    	    buff.flip();
    		session.write(buff);
    		//buff.clear();
    	}
    	return this;
    }
    
    public void close(){
        session.closeOnFlush();
    }
    
    public void closeNow() {
        session.closeNow();
    }
}
