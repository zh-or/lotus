package lotus.http.server;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import lotus.nio.Session;


public class HttpResponse {
	private static final int write_buffer_size		=	2048;
	
	private ByteBuffer					buff;
    private Session                     session;
    private ResponseStatus              status;
    private HashMap<String, String>     headers;
    private boolean						issendheader;
    
    public static HttpResponse defaultResponse(Session session, HttpRequest request){
    	HttpResponse response = new HttpResponse(session, ResponseStatus.SUCCESS_OK);
    	response.setHeader("Server", "simpli http server by lotus");
    	Calendar cal = Calendar.getInstance();
    	Date time = cal.getTime();
    	response.setHeader("Expires", time + "");
    	response.setHeader("Date", time + "");
    	String connection = request.getHeader("connection");
    	if(connection != null){
    		response.setHeader("Connection", connection);
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
        this.issendheader = false;
        this.buff = ByteBuffer.allocate(write_buffer_size);
        this.headers.put("Content-Type", "text/html");
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
    
    public void openSync(){
    	headers.put("Content-Encoding", "gzip");
    	headers.put("Transfer-Encoding", "chunked");
    	headers.remove("Content-Length");
    	sendHeader();
    }
    
    public void setHeader(String key, String value){
    	headers.put(key, value);
    }
    
    public void write(String str){
    	byte[] data = str.getBytes();
    	write(data);
    }
    
    public void write(byte[] b){
    	if(buff.capacity() - buff.limit() < b.length){/*需要扩容*/
    		byte[] data = Arrays.copyOf(buff.array(), buff.capacity() * 2);
    		buff = ByteBuffer.wrap(data);
    	}
    	buff.put(b);
    }
    
    private void sendHeader(){
    	if(issendheader) return;
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
    	issendheader = true;
    }
    
    public void flush(){
    	if(!issendheader){
    		setHeader("Content-Length", buff.position() + "");
    		sendHeader();
    	}
    	if(buff.limit() > 0){
    	    buff.flip();
    		session.write(buff);
    	}
    }
    
    public void close(){
        session.closeOnFlush();
    }
    
}
