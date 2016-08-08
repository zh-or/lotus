package lotus.socket.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.nio.IoHandler;
import lotus.nio.tcp.NioTcpServer;
import lotus.socket.common.LengthProtocolCode;


public class SocketServer {
	private NioTcpServer			     server;
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
    	server = new NioTcpServer(tcount, bufferlistmaxsize);
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
