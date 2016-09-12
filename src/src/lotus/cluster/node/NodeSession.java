package lotus.cluster.node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import lotus.cluster.Message;
import lotus.cluster.NetPack;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpClient;
import lotus.socket.client.SyncSocketClient;
import lotus.socket.common.LengthProtocolCode;
import lotus.util.Util;

public class NodeSession extends IoHandler{
    private static final String DEF_ENCRYPTION_KEY  =   "lotus-cluster-key";
    private static final String SESSION_USE_COUNT   =   "session-use-count";/*AtomicBoolean*/
    private static final int    SESSION_READ_CACHE  =   1024 * 1024;
    
    private int                 conn_max_size       =   100;/*连接数最大数量*/
    private int                 conn_low_size       =   10;/*连接数最小数量*/
    private int                 init_conn_timeout   =   30000;/*初始化连接超时时间*/
    
    private ReentrantLock       init_lock           =   new ReentrantLock();
    
    private String              host                =   "0.0.0.0";
    private int                 port                =   5000;
    private AtomicInteger       nowConnectionCount  =   new AtomicInteger(0);/*当前连接计数*/
    private NioTcpClient        client              =   null;/*data sockets*/
    private SyncSocketClient    cmdclient           =   null;
    private String              nodeid              =   null;
    private String              user_en_key         =   DEF_ENCRYPTION_KEY;//用户设置的密码
    private boolean             enableEncryption    =   false;
    private Charset             charset             =   Charset.forName("utf-8");
    private ArrayList<String>   subs                =   null;//当前订阅的消息类型
    private MessageHandler      handler             =   null;

    private ConcurrentHashMap<String, Session> sessions = null;
    /**
     * @param host service's address
     * @param port service's port
     * @throws IOException 
     */
    public NodeSession(String host, int port) throws IOException{
        this(host, port, Util.getUUID());
    }
    
    public NodeSession(String host, int port, String nodeid){
        this.nodeid = nodeid;
        this.host = host;
        this.port = port;
        this.subs = new ArrayList<String>();
        this.sessions = new ConcurrentHashMap<String, Session>();
        this.handler = new MessageHandler() {};
        this.client = new NioTcpClient(new LengthProtocolCode());
        this.client.setEventThreadPoolSize(0);
        this.client.setHandler(this);
        this.client.setSessionReadBufferSize(SESSION_READ_CACHE);
    }
    
    public synchronized boolean init(int timeout){
        try {
            client.init();
        } catch (IOException e) {
            return false;
        }
        cmdclient = new SyncSocketClient();
        cmdclient.setRecvTimeOut(30 * 1000);
        byte[] initdata = cmdclient.send(new NetPack(NetPack.CMD_INIT, nodeid.getBytes()).Encode());
        NetPack recv = new NetPack(initdata);
        
        if(getSomeConnection(conn_low_size) > 0){
            
            return true;
        }
        return false; 
    }
    
    public void setConnectionMaxSize(int size){
        this.conn_max_size = size;
    }
    
    public void setInitConnectionTimeout(int timeout){
        this.init_conn_timeout = timeout;
    }
    
    public String getNodeId(){
        return nodeid;
    }
    
    /**
     * 获取一些连接
     * @param size
     * @return
     */
    private int getSomeConnection(int size){
        int nowGettingconns = 0;
        int nowmax = conn_max_size - nowConnectionCount.get();
        if(size <= 0){
            size = nowmax < conn_low_size ? conn_low_size : nowmax; 
        }else if(size > nowmax){
            size = nowmax;
        }
        for(int i = 0; i < size; i++){
            Session t_session = client.connection(new InetSocketAddress(host, port), init_conn_timeout);
            if(t_session != null){
                nowConnectionCount.getAndIncrement();
                t_session.setAttr(SESSION_USE_COUNT, new AtomicBoolean(false));
                sessions.put(t_session.getId() + "", t_session);
                nowGettingconns++;
            }
        }
        return nowGettingconns;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized ArrayList<String> getSubscribeActions(){
        return (ArrayList<String>) subs.clone();
    }
    
    public boolean isInit(){
        return nowConnectionCount.get() > 0;
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
    }
    
    public synchronized void close(){
        subs.clear();
        Iterator<Entry<String, Session>> it = sessions.entrySet().iterator();
        Session t_session = null;
        while(it.hasNext()){
            Entry<String, Session> item = it.next();
            t_session = item.getValue();
            if(t_session != null){
                t_session.closeNow();
            }
        }
        nowConnectionCount.set(0);;
        sessions.clear();
        client.stop();
    }

    
    public void sendMessage(Message msg) throws Exception{
        msg.from = nodeid;
        sendPack(new NetPack(NetPack.CMD_MSG, Message.encode(msg, charset)));
    }
    
    /**
     * 同步方法
     * @param action
     */
    public synchronized boolean addSubscribe(String action) throws Exception{
        
        sendPack(new NetPack(NetPack.CMD_SUBS_MSG, action.getBytes(charset)));
        
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
        sendPack(new NetPack(NetPack.CMD_UNSUBS_MSG, action.getBytes(charset)));
        if(subs.contains(action)){
            return false;
        }
        return true;
    }
    
    private void sendPack(NetPack pack) throws Exception{
        byte[] data = pack.Encode();
        if(enableEncryption){
            data = Util.Encoded(data, user_en_key);
        }
    }
    
    @Override
    public void onClose(Session session) throws Exception {
        nowConnectionCount.getAndDecrement();
    }
    
    @Override
    public void onRecvMessage(Session session, Object msg) throws Exception {
        
    }
    
/*
    @Override
    public void onMessageRecv(SocketClient sc, byte[] msg) {
        if(enableEncryption){
            msg = Util.Decode(msg, encryptionKey);
        }
        NetPack pack = new NetPack(msg);
        byte type = pack.type;
        try {
            switch (type) {
                case NetPack.CMD_MSG:收到消息
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
                case NetPack.CMD_RES:收到消息回执
                    handler.onRecvMessageResponse(NodeSession.this, new MessageResult(pack.body, charset));
                    break;
                case NetPack.CMD_KEEP:心跳
                    
                    break;
                case NetPack.CMD_INIT:初始化反馈
                {
                    String rnodeid = new String(pack.body);
                    if(!Util.CheckNull(rnodeid) && rnodeid.equals(rnodeid)){
                        encryptionKey = rnodeid;
                        isinit = true;
                    }
                }
                    break;
                case NetPack.CMD_SUBS_MSG:订阅消息反馈
                {
                    String raction = new String(pack.body);
                    if(!Util.CheckNull(raction) && raction.equals(tmp_now)){
                        if(!subs.contains(raction)){
                            subs.add(raction);
                        }
                    }
                }
                    break;
                case NetPack.CMD_UNSUBS_MSG:取消订阅消息反馈
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
        
    }*/
    
}
