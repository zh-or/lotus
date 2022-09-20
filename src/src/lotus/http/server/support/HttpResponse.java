package lotus.http.server.support;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import lotus.json.JSONObject;
import lotus.nio.LotusIOBuffer;
import lotus.nio.Session;
import lotus.utils.Base64;
import lotus.utils.Utils;


public class HttpResponse {
	
	private LotusIOBuffer				buff                =   null;
    private Session                     session             =   null;
    private ResponseStatus              status              =   null;
    private HashMap<String, String>     headers             =   null;
    private boolean				        isSendHeader        =   false;
    private boolean                     isOpenSync          =   false;
    private Charset                     charset             =   null;
    private HttpRequest                 request             =   null;
    
    public static HttpResponse defaultResponse(Session session, HttpRequest request) {
    	HttpResponse response = new HttpResponse(session, request);
    	response.setHeader("Server", "lotus");
    	Date time = new Date();
    	response.setHeader("Expires", time.toString());
    	response.setHeader("Date", time.toString());
    	String connection = request.getHeader("connection");
    	if(connection != null){
    		response.setHeader("Connection", connection);
    	}
        response.setCharacterEncoding(request.getCharacterEncoding());
        
        if(request.isWebSocketConnection() && request.getContext().isOpenWebSocket()) {
            response.setStatus(ResponseStatus.INFORMATIONAL_SWITCHING_PROTOCOLS);
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
    
    /**
     * 默认编码为utf-8
     * @param pathname
     * @return
     */
    public static String filename2type(String pathname) {
        if(pathname.indexOf(".js") != -1 ){
            return "application/javascript; charset=utf-8";
        }
        if(pathname.indexOf(".html") != -1 ){
            return "text/html; charset=utf-8";
        }
        if(pathname.indexOf(".gif") != -1 ){
            return "image/gif";
        }
        if(pathname.indexOf(".png") != -1 ){
            return "image/png";
        }
        if(pathname.indexOf(".jpg") != -1 ){
            return "image/jpg";
        }
        return "";
    }

    public HttpResponse(Session session, HttpRequest request) {
        this.session = session;
        this.request = request;
        status = ResponseStatus.SUCCESS_OK;
        headers = new HashMap<String, String>();
        isSendHeader = false;
        buff = new LotusIOBuffer(session.getContext());
        headers.put("Content-Type", "text/html");
    }
    
    public void setCharacterEncoding(Charset newCharset){
        charset = newCharset;
    }
    
    public void setStatus(ResponseStatus newStatus){
        status = newStatus;
    }
    
    /**
     * 发送302跳转
     * @param path
     */
    public void sendRedirect(String path) {
        this.status = ResponseStatus.REDIRECTION_FOUND;
        headers.put("Location", path);
    //    headers.put("Connection", "close");
        sendHeader();
    }
    
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }
    
    public HttpResponse openSync() {
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
    public HttpResponse write(Object obj){
        write(obj.toString());
        return this;
    }
    
    public HttpResponse write(String str){
    	byte[] data = str.getBytes(charset);
    	write(data);
    	return this;
    }
    
    public HttpResponse write(byte[] b) {
        int len = b.length;
        String hexlen = Integer.toHexString(len);
        if(isOpenSync) {
            len += 4;
            len += hexlen.length();
        }
    	
    	if(isOpenSync) {
    	    buff.append(hexlen.getBytes());
    	    buff.append("\r\n".getBytes());
    	}
    	buff.append(b);
    	if(isOpenSync) {
            buff.append("\r\n".getBytes());
    	}
    	return this;
    }
    
    public HttpResponse write(ByteBuffer b) {
        int len = b.position();
        String hexlen = Integer.toHexString(len);
        if(isOpenSync) {
            len += 4;
            len += hexlen.length();
        }
    	
    	if(isOpenSync) {
    	    buff.append(hexlen.getBytes());
    	    buff.append("\r\n".getBytes());
    	}
    	buff.append(b);
    	if(isOpenSync) {
            buff.append("\r\n".getBytes());
    	}
    	return this;
    }

    /**
     * 此方法在一个请求中只能调用一次, 如需多次写入 请调用 write 方法,
     * 小文件直接全部读出来一次发送会比较好, 此方法适用发送大文件(http时)
     * 此方法可能会导致文件被锁住,
     * @param file
     * @throws IOException 
     */
    public void sendFile(File file) throws Exception {
        
        if(!isSendHeader) {
            setHeader("Content-Length", file.length() + "");
            sendHeader();
        }
        session.write(new HttpMessageWrap(HttpMessageWrap.HTTP_MESSAGE_TYPE_FILE, file));
    }
    
    public void syncEnd() {
        isOpenSync = false;
        write("0\r\n\r\n");
        flush();
    }
    
    private HttpResponse sendHeader() {
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
    	byte[] bytes = sb.toString().getBytes(charset);
    	ByteBuffer buffer = session.getWriteCacheBuffer(bytes.length);
    	buffer.put(bytes);
    	session.write(new HttpMessageWrap(HttpMessageWrap.HTTP_MESSAGE_TYPE_HEADER, buffer));
    	isSendHeader = true;
    	return this;
    }
    
    public HttpResponse flush() {
        int len = buff.getDataLength();
    	if(!isSendHeader) {
    		setHeader("Content-Length", len + "");
    		sendHeader();
    	}
    	if(len > 0){
            session.write(new HttpMessageWrap(HttpMessageWrap.HTTP_MESSAGE_TYPE_BUFFER, buff));
            buff = new LotusIOBuffer(session.getContext());
    		//buff.clear();
    	}
    	return this;
    }
    
    public void free() {
        buff.free();
        buff = null;
    }
    
    public HttpRequest getRequest() {
        return request;
    }
    
    public void close(){
        session.closeOnFlush();
    }
    
    public void closeNow() {
        session.closeNow();
    }
}
