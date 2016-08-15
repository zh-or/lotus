package lotus.socket.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.nio.IoHandler;
import lotus.nio.tcp.NioTcpServer;
import lotus.socket.common.LengthProtocolCode;


public class SocketServer {
	private NioTcpServer			 server;
	private IoHandler				 handler;
    private String                   host;
    private int                      port;
    private int                      EventThreadPoolSize = 100;
    private int                      readbuffsize = 1024 * 2;
    private int                      idletime = 0;
    private int 					 bufferlistmaxsize = 1024;
    private int  				     sockettimeout = 1000 * 10;
    
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
    public SocketServer(String host, int port){
        this.host = host;
        this.port = port;
    }
    
    public void setHandler(IoHandler handler) {
        this.handler = handler;
    }

    public void setEventThreadPoolSize(int size) {
        this.EventThreadPoolSize = size;
    }

    public void setReadbuffsize(int size) {
        this.readbuffsize = size;
    }
    
    public void setIdletime(int idletime) {
        this.idletime = idletime;
    }

    public void setReadBufferCacheListSize(int size) {
        this.bufferlistmaxsize = size;
    }

    public void setSockettimeout(int sockettimeout) {
        this.sockettimeout = sockettimeout;
    }

    public void start() throws IOException{
    	server = new NioTcpServer();
    	server.setReadBufferCacheListSize(bufferlistmaxsize);
    	server.setEventThreadPoolSize(EventThreadPoolSize);
    	server.setSessionReadBufferSize(readbuffsize);
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
    
}
