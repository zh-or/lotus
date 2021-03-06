package lotus.socket.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.nio.IoHandler;
import lotus.nio.LengthProtocolCode;
import lotus.nio.tcp.NioTcpServer;


public class SocketServer {
	private NioTcpServer			 server;
	private IoHandler				 handler;
	private InetSocketAddress        addr;
    private int                      EventThreadPoolSize    = 100;
    private int                      readbuffsize           = 1024 * 2;
    private int                      idletime               = 0;
    private int 					 bufferlistmaxsize      = 1024;
    private int  				     soTimeout              = 1000 * 10;
    
    /**
     * @param host 
     * @param port 
     */
    public SocketServer(InetSocketAddress addr){
        this.addr = addr;
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
    
    /**
     * 设置连接空闲检测
     * @param idletime 单位 秒
     */
    public void setIdletime(int idletime) {
        this.idletime = idletime * 1000;
    }

    public void setReadBufferCacheListSize(int size) {
        this.bufferlistmaxsize = size;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public void start() throws IOException{
    	server = new NioTcpServer();
    	server.setReadBufferCacheListSize(bufferlistmaxsize);
    	server.setEventThreadPoolSize(EventThreadPoolSize);
    	server.setSessionCacheBufferSize(readbuffsize);
    	server.setSessionCacheBufferSize(readbuffsize);
    	server.setSessionIdleTime(idletime);
    	server.setSoTimeOut(soTimeout);
    	server.setProtocolCodec(new LengthProtocolCode());
    	server.setHandler(handler);
        server.start(addr);
    }
    
    public void stop(){
    	server.close();
    	server = null;
    }
    
    public int getPort(){
        return addr.getPort();
    }
    
}
