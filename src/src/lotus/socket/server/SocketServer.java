package lotus.socket.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.TcpServer;
import lotus.socket.common.LengthProtocolCode;
import lotus.util.Util;


public class SocketServer {
	private TcpServer			     server;
	private IoHandler				 handler;
    private String                   host;
    private int                      port;
    private int                      tcount;
    private int                      readbuffsize;
    private int                      idletime;
    private int 					 bufferlistmaxsize;
    private int  				     sockettimeout;
    
    /**
     * @param host 
     * @param port 
     * @param tcount 
     * @param readbuffersize 读缓冲区大小
     * @param ideatime 单位'秒'
     * @param buffer_list_maxsize 换型缓冲队列最大长度
     * @param handler 
     * @throws Exception
     */
    public SocketServer(String host, int port, int tcount, int read_buffer_size, int idletime, int buffer_list_maxsize, int sockettimeout, IoHandler handler){
        this.host = host;
        this.port = port;
        this.tcount = tcount;
        this.handler = handler;
        this.readbuffsize = read_buffer_size;
        this.idletime = idletime * 1000;
        this.bufferlistmaxsize = buffer_list_maxsize;
        this.sockettimeout = sockettimeout;
    }
    
    public void start() throws IOException{
    	server = new TcpServer(0, tcount, bufferlistmaxsize);
    	server.setSessionReadBufferSize(readbuffsize);
    	server.setSessionIdleTime(idletime);
    	server.setSocketTimeOut(sockettimeout);
    	server.setProtocolCodec(new LengthProtocolCode());
    	server.setHandler(handler);
        server.bind(new InetSocketAddress(host, port));
    }
    
    public void stop(){
    	server.unbind();
    	server = null;
    }
    
    public int getPort(){
        return port;
    }
    
    
    /**
     * @param session
     * @param type
     * @param deviceMark
     * @param content
     */
    public static void send(Session session, byte[] content){
        send(session, content, false);
    }
    
    /**
     * @param session
     * @param type
     * @param deviceMark
     * @param content
     * @param isclose 发送完成后是否关闭该链接
     */
    public static void send(Session session, byte[] content, boolean isclose){
        
        byte[] send =  new byte[content.length + 2 + 2];
        send[0] = 0x02;
        send[send.length - 1] = 0x03;
        byte[] len = Util.short2byte(send.length);

        send[1] = len[0];
        send[2] = len[1];

        System.arraycopy(content, 0, send, 3, content.length);
        ByteBuffer buff = ByteBuffer.wrap(send);
        session.write(buff);
        if(isclose){
        	session.closeOnFlush();
        }
    }
}
