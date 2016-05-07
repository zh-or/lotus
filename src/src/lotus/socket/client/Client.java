package lotus.socket.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lotus.json.JSONObject;
import lotus.socket.common.ClientCallback;
import lotus.util.Util;

/**
 * 客户端 这个类要重写
 * @author or
 */
public class Client {

    private Socket              socket                   = null;
    private ClientCallback      callback                 = null;
    private ExecutorService     sendpool                 = null;
    private ExecutorService     eventpool                = null;
    private OutputStream        out                      = null;
    private InputStream         in                       = null;
    private long                lasthtime                = 0;
    private int                 keepalive                = 0;
    private byte[]              keepcontent              = {};
    private Object              attr                     = null;//辅助参数
    private String              host                     = "0.0.0.0";
    private int                 port                     = 5000;
    private int                 timeout                  = 10000;
    private int                 RECV_BUFF_SIZE           = 1024 * 2;
    
    public Client(String host, int port, int timeout, ClientCallback callback){
        this.callback = callback;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        sendpool = Executors.newSingleThreadExecutor();
        eventpool = Executors.newSingleThreadExecutor();
        
    }
    
    public Object getAttribute() {
        return attr;
    }

    public void setAttribute(Object attr) {
        this.attr = attr;
    }
    
    public boolean isconn(){
        if(socket != null && socket.isConnected()){
            return true;
        }
        return false;
    }
    
    /**
     * 设置心跳间隔单位 ‘秒’
     * @param keepalive 0 为关闭
     * @param keepcontent 心跳时发送的内容
     */
    public void setKeepalive(int keepalive, byte[] keepcontent){
        this.keepalive = keepalive * 1000;
        this.keepcontent = keepcontent;
    }
    
    public void setRecvBufferSize(int size){
        this.RECV_BUFF_SIZE = size;
    }
    
    
    /**
     * @param host
     * @param port
     * @param timeout used in milliseconds.
     */
    public boolean connection(){
        try {
            socket = new Socket();
            socket.setSoTimeout(1000);/*socket 读超时时间*/
            socket.setReceiveBufferSize(RECV_BUFF_SIZE);
            socket.connect(new InetSocketAddress(host, port), timeout);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            Thread recvt = new Thread(new RecvThread());
            recvt.setName("socket recv thread");
            recvt.start();
        } catch (Exception e) {}
        return isconn();
    }
    
    public synchronized void close(){
        if(isclosed()) return;
        try {
            if(in != null){
                in.close();
                in = null;
            }
        } catch (Exception e) {}
        try {
            if(out != null){
                out.close();
                out = null;
            }
        } catch (Exception e) {}
        try {
            if(socket != null){
                socket.close();
                socket = null;
            }
        } catch (Exception e) {}
        eventpool.execute(new EventRun(Client.this, callback));
    }
    
    public void shutdown(){
        sendpool.shutdown();
    }
    
    public synchronized boolean isclosed(){ 
        return socket == null || socket.isClosed();
    }
    
    public void send(byte[] content){
        sendpool.execute(new SendThread(content));
    }
    
    public void send(String content){
        send(content.getBytes());
    }
    
    public void send(JSONObject json){
        send(json.toString());
    }
    
    private class RecvThread implements Runnable{
        
        @Override
        public void run() {
            byte[] head = new byte[3];
            while(true){
                if(socket != null && !socket.isClosed() && !socket.isInputShutdown()){
                    try {
                        if(in.read(head) == -1){
                            break;
                        }
                        if(head[0] == 0x02){/*是包头*/
                            int length = Util.byte2short(head, 1);
                            if(length < 65535){
                                length -= 3;
                                byte[] content = new byte[length - 1];
                                in.read(content);
                                if(in.read() == 0x03){
                                    eventpool.execute(new EventRun(Client.this, content, callback));
                                    lasthtime = System.currentTimeMillis();
                                }
                            }else{
                                if(!isclosed()) close();
                            }
                        }
                        head[0] = 0;
                    } catch (Exception e) {/*此处为读超时异常，无需处理。*/}
                    if(keepalive > 0 && keepcontent != null){//检查心跳时间是否已到
                        long now = System.currentTimeMillis();
                        if(now - lasthtime >= keepalive && keepcontent != null){
                            send(keepcontent);
                        }
                    }
                    continue;
                }else{
                    break;
                }
                
            }

            if(!isclosed()) close();
        }
        
    }
    
    private class SendThread implements Runnable{
        
        private byte[] content;
        
        public SendThread(byte[] content){
            this.content = content;
        }

        @Override
        public void run() {
            if(out != null && socket != null && !socket.isOutputShutdown()){
                try {
                    int len = content.length;
                    out.write(0x02);
                    out.write(Util.short2byte(len + 4));
                    out.write(content);
                    out.write(0x03);
                    out.flush();
                    lasthtime = System.currentTimeMillis();
                    eventpool.execute(new EventRun(Client.this, true, content, callback));
                    return ;
                } catch (Exception e) {}
            }
            eventpool.execute(new EventRun(Client.this, false, content, callback));
            if(!isclosed()) close();
        }
    }
    
    private class EventRun implements Runnable{/*虽然烂, 不改了*/
        private int type = 0;
        private boolean isok = false;
        private byte[] data = null;
        private Client _this;
        
        private ClientCallback _callback;
        
        /**
         * close
         */
        public EventRun(Client _this, ClientCallback callback){
            this._callback = callback;
            this._this = _this;
            type = 0;
        }
        
        /**
         * recv
         */
        public EventRun(Client _this, byte[] msg, ClientCallback callback){
            this._callback = callback;
            this._this = _this;
            this.data = msg;
            type = 1;
        }
        
        /**
         * sent
         */
        public EventRun(Client _this, boolean isok, byte[] msg, ClientCallback callback){
            this._callback = callback;
            this._this = _this;
            this.isok = isok;
            this.data = msg;
            type = 2;
        }
        
        @Override
        public void run() {
            
            switch(type){
                case 0:
                    _callback.onClose(_this);
                    break;
                case 1:
                    _callback.onMessageRecv(_this, data);
                    break;
                case 2:
                    _callback.onSendt(_this, isok, data);
                    break;
            }
        }
    }
}
