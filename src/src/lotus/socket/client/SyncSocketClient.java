package lotus.socket.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import lotus.utils.Utils;

/**
 * 同步socket
 * @author OR
 */
public class SyncSocketClient {
	private Socket              socket                   = null;
    private Object              attr                     = null;//辅助参数
    private int                 RECV_BUFF_SIZE           = 1024 * 2;
    private int                 recv_time_out            = 10 * 1000;

    public SyncSocketClient(){ }
    
    public void setRecvTimeOut(int recv_time_out){
        this.recv_time_out = recv_time_out;
    }
    
    public Object getAttr() {
        return attr;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }
    
    public void setRecvBufferSize(int size){
        this.RECV_BUFF_SIZE = size;
    }
    
    /**
     * @param address
     * @param port
     * @param timeout 连接超时时间
     * @return
     */
	public boolean connection(InetSocketAddress serveraddress, int timeout){
	    close();
	    try {
            socket = new Socket();
            socket.setSoTimeout(recv_time_out);
            socket.setReceiveBufferSize(RECV_BUFF_SIZE);
            socket.setKeepAlive(false);
            socket.setTcpNoDelay(true);
            socket.connect(serveraddress, timeout);
            return socket.isConnected();
        } catch (Exception e) {}
	    
	    return false;
	}
	
	public byte[] send(final byte[] data){
	    
	    if(socket == null || socket.isOutputShutdown()){
            close();
            return null;
        }
	    if(socket.isConnected() == false){
	        return null;
	    }
        try {
            OutputStream out = socket.getOutputStream();
            int len = data.length + 4;
            out.write(0x02);
            out.write(Utils.short2byte(len));
            out.write(data);
            out.write(0x03);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
        return recv();
	}

    private byte[] head = new byte[3];
    
	public byte[] recv(){
	    if(socket == null || socket.isInputShutdown()){
            close();
            return null;
        }
	    try {
	        InputStream in = socket.getInputStream();
	        if(in.read(head, 0, 3) == 3 && head[0] == 0x02){
	            int len = Utils.byte2short(head, 1);
                if(len < 65535){
                    len -= 4;
                    byte[] content = new byte[len];
                    if(in.read(content) != -1 && in.read() == 0x03){
                        return content;
                    }
                }
            }
	        close();
            return null;
        } catch (Exception e) { }
        return null;/*这里不close是因为有可能是读超时了*/
	}
	
	public void close(){
	    if(socket != null){
	        try {
	            socket.close();
	            socket = null;
            } catch (Exception e) {}
	    }
	}
	
	public boolean isconnectioned(){
	    if(socket != null || socket.isConnected())
	        return true;
	    return false;
	}
	
	
}
