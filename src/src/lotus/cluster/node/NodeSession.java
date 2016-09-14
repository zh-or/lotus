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

import com.sun.swing.internal.plaf.synth.resources.synth;

import lotus.cluster.Message;
import lotus.cluster.NetPack;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpClient;
import lotus.socket.client.SyncSocketClient;
import lotus.socket.common.LengthProtocolCode;
import lotus.util.Util;

public class NodeSession {
    private static final String DEF_ENCRYPTION_KEY  =   "lotus-cluster-key";
    private static final String SESSION_USE_COUNT   =   "session-use-count";/*AtomicBoolean*/
    private static final String SESSION_IS_INIT     =   "isinit";
    private static final int    SESSION_READ_CACHE  =   1024 * 1024;
    
    private int                 conn_max_size       =   100;/*连接数最大数量*/
    private int                 conn_min_size       =   10;/*连接数最小数量*/
    
    private ReentrantLock       conn_lock           =   new ReentrantLock();
    private Object              init_wait           =   new Object();
    private InetSocketAddress   serveraddress       =   null;
    private AtomicInteger       nowConnectionCount  =   new AtomicInteger(0);/*当前连接计数*/
    private NioTcpClient        client_data         =   null;/*data sockets*/
    private SyncSocketClient    client_cmd          =   null;
    private String              nodeid              =   null;
    private String              user_en_key         =   DEF_ENCRYPTION_KEY;//用户设置的密码
    private boolean             enableEncryption    =   false;
    private Charset             charset             =   Charset.forName("utf-8");
    private ArrayList<String>   subs                =   null;//当前订阅的消息类型
    private MessageHandler      handler             =   null;

    private Session[]           sessions            =   null;
    private int                 session_capacity    =   -1;
    private int                 session_bound       =   0;
    
    /**
     * @param host service's address
     * @param port service's port
     * @throws IOException 
     */
    public NodeSession(InetSocketAddress serveraddress) throws IOException{
        this(serveraddress, Util.getUUID());
    }
    
    public NodeSession(InetSocketAddress serveraddress, String nodeid){
        this.nodeid = nodeid;
        this.serveraddress = serveraddress;
        this.subs = new ArrayList<String>();
        this.sessions = new Session[conn_max_size];
        this.handler = new MessageHandler() {};
        this.client_data = new NioTcpClient(new LengthProtocolCode());
        this.client_data.setEventThreadPoolSize(conn_max_size);
        this.client_data.setHandler(new DataHandler());
        this.client_data.setSessionReadBufferSize(SESSION_READ_CACHE);
    }
    
    /**
     * 初始化
     * @param timeout 0 为不等待, 等待的毫秒数
     * @return 返回初始化的连接数 -1 表示连接服务器失败
     */
    public synchronized int init(long timeout){

        try {
            client_cmd = new SyncSocketClient();
            client_cmd.setRecvTimeOut(30 * 1000);
            if(client_cmd.connection(serveraddress, (int)timeout) == false){
                
                return -1;
            }
            
            byte[] initdata = send_cmd(new NetPack(NetPack.CMD_INIT, nodeid.getBytes()).Encode());
            if(initdata == null) {
                return -1;
            }
            NetPack recv = new NetPack(initdata);
            if(recv.type == NetPack.CMD_INIT){
                this.nodeid = new String(recv.body);
                if(Util.CheckNull(this.nodeid)) return 0;
                client_data.init();
                getSomeConnection(conn_min_size);
                if(timeout > 0){
                    synchronized (init_wait) {
                        try {
                            init_wait.wait(timeout);
                        } catch (Exception e) {}
                    }
                }
                return nowConnectionCount.get();
            }
        } catch (IOException e) {}
        return 0; 
    }
    
    /**
     * 获取一些连接
     * @param size
     * @return 此处返回的连接数不代表最终连接数
     */
    private synchronized int getSomeConnection(int size){
        int nowGettingconns = 0;
        int nowmax = conn_max_size - nowConnectionCount.get();
        if(size <= 0){
            size = nowmax < conn_min_size ? conn_min_size : nowmax; 
        }else if(size > nowmax){
            size = nowmax;
        }
        
        for(int i = 0; i < size; i++){
            Session t_session = client_data.connection(serveraddress, 0);
            if(t_session != null){
                //nowConnectionCount.getAndIncrement();
                //t_session.setAttr(SESSION_USE_COUNT, new AtomicBoolean(false));
               
                nowGettingconns++;
            }
        }
        return nowGettingconns;
    }
    
    private void send_data(Session session, byte[] data){
        if(enableEncryption){
            data = Util.Encoded(data, user_en_key);
        }
        session.write(data);
    }
    
    
    private byte[] send_cmd(byte[] data){
        if(enableEncryption){
            data = Util.Encoded(data, user_en_key);
        }
        return client_cmd.send(data);
    }
    
    /**
     * 此方法不要在初始化后调用 无效
     * @param size
     */
    public void setConnectionMaxSize(int size){
        if(nowConnectionCount.get() > 0) return;
        this.conn_max_size = size;
        this.sessions = new Session[size];
    }
        
    public void setConnectionMinSize(int size){
        this.conn_min_size = size;
    }
    
    public String getNodeId(){
        return nodeid;
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
        for(int i = 0; i < session_capacity; i++){
            if(sessions[i] != null){
                sessions[i].closeNow();
                sessions[i] = null;
            }
        }
        nowConnectionCount.set(0);;
        session_capacity = -1;
        session_bound = 0;
        client_data.stop();
    }

    /**
     * @param action
     */
    public synchronized boolean addSubscribe(String action) throws Exception{
        
        return true;
    }
    
    /**
     * @param action
     */
    public synchronized boolean removeSubscribe(String action) throws Exception{
        
        return true;
    }
    
    
    private class DataHandler extends IoHandler{
        
        @Override
        public void onConnection(Session session) throws Exception {
            send_data(session, new NetPack(NetPack.CMD_DATA_INIT, nodeid.getBytes(charset)).Encode());
            
        }
        
        @Override
        public void onClose(Session session) throws Exception {
            Object obj = session.getAttr(SESSION_IS_INIT);
            if(obj != null){
                conn_lock.lock();
                try {
                    for(int i = 0; i < session_capacity; i++){
                        if(sessions[i] == session){
                            System.arraycopy(sessions, i + 1, sessions, i, session_capacity - i);
                            session_capacity--;
                        }
                    }
                } catch (Exception e) {
                    
                }finally{
                    conn_lock.unlock();
                }
                nowConnectionCount.getAndDecrement();
            }
           
        }
        
        @Override
        public void onRecvMessage(Session session, Object msg) throws Exception {
            if(msg instanceof byte[]){
                NetPack pack = new NetPack((byte[]) msg);
                byte packtype = pack.type;
                
                switch (packtype) {
                    case NetPack.CMD_DATA_INIT:
                    {
                        session.setAttr(SESSION_IS_INIT, "1");
                        conn_lock.lock();
                        try {
                            sessions[++session_capacity] = session;
                        } catch (Exception e) {
                            
                        }finally{
                            conn_lock.unlock();
                        }
                        nowConnectionCount.getAndIncrement();
                        if(nowConnectionCount.get() >= conn_min_size){
                            synchronized (init_wait) {
                                init_wait.notifyAll();
                            }
                        }
                        break;
                    }
                    default:
                        /*未知的数据包*/
                        break;
                }
                
            }else{
                session.closeNow();
            }
            
        }
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
