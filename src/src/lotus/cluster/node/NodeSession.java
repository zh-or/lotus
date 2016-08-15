package lotus.cluster.node;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import lotus.cluster.Message;
import lotus.cluster.MessageFactory;
import lotus.cluster.MessageResult;
import lotus.cluster.NetPack;
import lotus.nio.IoHandler;
import lotus.nio.tcp.NioTcpClient;
import lotus.socket.client.SocketClient;
import lotus.socket.common.ClientCallback;
import lotus.socket.common.LengthProtocolCode;
import lotus.util.Util;

public class NodeSession extends IoHandler{
    private static final String DEF_ENCRYPTION_KEY  =   "lotus-cluster-key";
    private static final int    SESSION_READ_CACHE  =   1024 * 1024;
    
    private String              host                =   "0.0.0.0";
    private int                 port                =   5000;
    private NioTcpClient        client              =   null;
    private String              nodeid              =   null;
    private String              user_en_key         =   DEF_ENCRYPTION_KEY;//用户设置的密码
    private String              use_en_key          =   user_en_key;//当前使用的密码
    private Charset             charset             =   Charset.forName("utf-8");
    private boolean             isinit              =   false;
    private boolean             enableEncryption    =   false;
    private ArrayList<String>   subs                =   null;//当前订阅的消息类型
    private String              tmp_now             =   null;
    private MessageHandler      handler             =   null;
    
    /**
     * @param host service's address
     * @param port service's port
     * @throws IOException 
     */
    public NodeSession(String host, int port) throws IOException{
        this(host, port, Util.getUUID());
    }
    
    public NodeSession(String host, int port, String nodeid) throws IOException{
        this.nodeid = nodeid;
        this.host = host;
        this.port = port;
        this.subs = new ArrayList<String>();
        this.handler = new MessageHandler() {};
        this.client = new NioTcpClient(new LengthProtocolCode());
        this.client.setHandler(this);
        this.client.setSessionReadBufferSize(SESSION_READ_CACHE);
        
    }
    
    public synchronized boolean init(int timeout){
        isinit = false;
        try {
        	
            
        } catch (Exception e) {}
        return false;
    }
    
    public String getNodeId(){
        return nodeid;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized ArrayList<String> getSubscribeActions(){
        return (ArrayList<String>) subs.clone();
    }
    
    public boolean isInit(){
        return isinit;
    }
    
    public void setMessageCharset(String charsetName){
        this.charset = Charset.forName(charsetName);
    }
    
    public void setHandler(MessageHandler handler){
        this.handler = handler;
    }
    
    public void setEnableEncryption(boolean isenable, String key){
        this.enableEncryption = isenable;
        this.user_en_key = key;
        if(Util.CheckNull(key)){
            this.user_en_key = DEF_ENCRYPTION_KEY;
        }
        this.use_en_key = this.user_en_key;
    }
    
    public synchronized void close(){
        
        subs.clear();
        use_en_key = user_en_key;
        isinit = false;
    }
    
    private void _wait(int timeout){
        synchronized (this) {
            try {
                this.wait(timeout);
            } catch (InterruptedException e) {}
        }
    }
    
    private void _notifyAll(){
        synchronized (this) {
            this.notifyAll();
        }
    }
    
    public void sendMessage(Message msg) throws Exception{
        msg.from = nodeid;
        sendPack(new NetPack(NetPack.CMD_MSG, MessageFactory.encode(msg, charset)));
    }
    
    /**
     * 同步方法
     * @param action
     */
    public synchronized boolean addSubscribe(String action) throws Exception{
        tmp_now = action;
        sendPack(new NetPack(NetPack.CMD_SUBS_MSG, action.getBytes(charset)));
        _wait(5000);
        if(!subs.contains(action)){
            return false;
        }
        return true;
    }
    
    /**
     * 同步方法
     * @param action
     */
    public synchronized boolean removeSubscribe(String action) throws Exception{
        tmp_now = action;
        sendPack(new NetPack(NetPack.CMD_UNSUBS_MSG, action.getBytes(charset)));
        _wait(5000);
        if(subs.contains(action)){
            return false;
        }
        return true;
    }
    
    private void sendPack(NetPack pack)throws Exception{
        if(client == null || pack == null){
            throw new Exception("null");
        }
        byte[] data = pack.Encode();
        if(enableEncryption){
            data = Util.Encoded(data, use_en_key);
        }
        
    }

    @Override
    public void onMessageRecv(SocketClient sc, byte[] msg) {
        if(enableEncryption){
            msg = Util.Decode(msg, encryptionKey);
        }
        NetPack pack = new NetPack(msg);
        byte type = pack.type;
        try {
            switch (type) {
                case NetPack.CMD_MSG:/*收到消息*/
                {
                        Message tmsg = MessageFactory.decode(pack.body, charset);
                        byte mtype = tmsg.type;
                        switch (mtype) {
                            case Message.MTYPE_BROADCAT:
                                handler.onRecvBroadcast(NodeSession.this, tmsg);
                                break;
                            case Message.MTYPE_MESSAGE:
                                handler.onRecvMessage(NodeSession.this, tmsg);
                                break;
                            case Message.MTYPE_SUBSCRIBE:
                                handler.onRecvSubscribe(NodeSession.this, tmsg);
                                break;
                        }
                        msgfactory.destory(tmsg);
                }
                    break;
                case NetPack.CMD_RES:/*收到消息回执*/
                    handler.onRecvMessageResponse(NodeSession.this, new MessageResult(pack.body, charset));
                    break;
                case NetPack.CMD_KEEP:/*心跳*/
                    
                    break;
                case NetPack.CMD_INIT:/*初始化反馈*/
                {
                    String rnodeid = new String(pack.body);
                    if(!Util.CheckNull(rnodeid) && rnodeid.equals(rnodeid)){
                        encryptionKey = rnodeid;
                        isinit = true;
                    }
                }
                    break;
                case NetPack.CMD_SUBS_MSG:/*订阅消息反馈*/
                {
                    String raction = new String(pack.body);
                    if(!Util.CheckNull(raction) && raction.equals(tmp_now)){
                        if(!subs.contains(raction)){
                            subs.add(raction);
                        }
                    }
                }
                    break;
                case NetPack.CMD_UNSUBS_MSG:/*取消订阅消息反馈*/
                {
                    String raction = new String(pack.body);
                    if(!Util.CheckNull(raction) && raction.equals(tmp_now)){
                        if(subs.contains(raction)){
                            subs.remove(raction);
                        }
                    }
                }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            switch (type) {
                case NetPack.CMD_INIT:
                case NetPack.CMD_SUBS_MSG:
                case NetPack.CMD_UNSUBS_MSG:
                    _notifyAll();
                    break;
            }
        }
        
    }
    
}
