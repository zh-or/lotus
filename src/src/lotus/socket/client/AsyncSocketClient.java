package lotus.socket.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lotus.socket.common.ClientCallback;
import lotus.socket.common.EventRunnable;
import lotus.util.Util;

/**
 * 异步socket
 * @author OR
 */
public class AsyncSocketClient {
	private Socket              socket                   = null;
    private ClientCallback      callback                 = null;
    private ExecutorService     sendpool                 = null;
    private ExecutorService     eventpool                = null;
    private long                lasthtime                = 0;
    private int                 keepalive                = 180 * 1000;
    private byte[]              keepcontent              = {0x00};
    private Object              attr                     = null;//辅助参数
    private int                 RECV_BUFF_SIZE           = 1024 * 2;
    private int                 read_time_out            = 10 * 1000;
    private volatile boolean    closed                   = false;

    public AsyncSocketClient(ClientCallback callback){
        this.callback = callback;
        sendpool = Executors.newSingleThreadExecutor();
        eventpool = Executors.newSingleThreadExecutor();
    }
    
    public Object getAttr() {
        return attr;
    }
    
    public void setReadTimeOut(int read_time_out){
        this.read_time_out = read_time_out;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }

    public ExecutorService getEventExecService(){
        return eventpool;
    }
    
    public void setRecvBufferSize(int size){
        this.RECV_BUFF_SIZE = size;
    }
    
    /**
     * 设置心跳间隔, 若设置时间大于0, 当socket空闲时间到的时候则会像服务器发送 0x00 
     * @param time
     */
    public void setKeepLive(int time){
        this.keepalive = time;
        if(socket != null && time > 0){
            try {
                socket.setSoTimeout(time / 3);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * @param address
     * @param port
     * @param timeout 连接超时时间
     * @return
     */
	public boolean connection(String hostname, int port, int timeout){
	    close();
	    try {
	        closed = false;
            socket = new Socket();
            socket.setReceiveBufferSize(RECV_BUFF_SIZE);
            lasthtime = System.currentTimeMillis();
            socket.setSoTimeout(read_time_out);
            socket.connect(new InetSocketAddress(hostname, port), timeout);
            Thread thread_recv = new Thread(new RecvThread());
            thread_recv.setName("socket recv thread");
            thread_recv.start();
            return true;
        } catch (Exception e) {}
	    
	    return false;
	}
	
	public void send(final byte[] data){
	    sendpool.execute(new Runnable() {
            @Override
            public void run() {
                if(closed || socket == null || socket.isOutputShutdown()){
                    close();
                    return;
                }
                try {
                    OutputStream out = socket.getOutputStream();
                    int len = data.length + 4;
                    out.write(0x02);
                    out.write(Util.short2byte(len));
                    out.write(data);
                    out.write(0x03);
                    out.flush();
                    lasthtime = System.currentTimeMillis();
                } catch (Exception e) {
                    close();
                }
            }
        });
	}
	
	public void close(){
	    if(closed) return;
	    closed = true;
	    
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
	
	
	public class RecvThread implements Runnable{

        @Override
        public void run() {
            byte[] head = new byte[3];
            try{
                while(true){
                    if(socket != null && !socket.isClosed() && !socket.isInputShutdown()){
                        InputStream in = socket.getInputStream();
                        try {
                            if(in.read(head, 0, 3) == -1){
                                break;
                            }
                            if(head[0] == 0x02){
                                int len = Util.byte2short(head, 1);
                                if(len < 65535){
                                    len -= 4;
                                    byte[] content = new byte[len];
                                    if(in.read(content) == -1){
                                        break;
                                    }
                                    if(in.read() == 0x03){
                                        eventpool.execute(new EventRunnable(ClientCallback.EventType.ONMESSAGERECV, callback, AsyncSocketClient.this, content));
                                        lasthtime = System.currentTimeMillis();
                                        continue;
                                    }
                                }
                            }
                            break;
                        } catch (IOException e) { /*此处为读超时异常，无需处理。*/ }
                        if(keepalive > 0 && keepcontent != null){//检查心跳时间是否已到
                            long now = System.currentTimeMillis();
                            if(now - lasthtime >= keepalive && keepcontent != null){
                                send(keepcontent);
                            }
                        }
                    }else{
                        break;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            eventpool.execute(new EventRunnable(ClientCallback.EventType.ONCLOSE, callback, AsyncSocketClient.this, null));
            close();
        }
	}
	
}
