package lotus.cluster.node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import lotus.cluster.Message;
import lotus.cluster.MessageResult;
import lotus.cluster.NetPack;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpClient;
import lotus.socket.client.SyncSocketClient;
import lotus.socket.common.LengthProtocolCode;
import lotus.util.Util;

public class NodeSession {
    private static final String DEF_ENCRYPTION_KEY  =   "lotus-cluster-key";
    private static final String SESSION_IS_INIT     =   "isinit";
    private static final int    SESSION_READ_CACHE  =   1024 * 1024;
    
    private int                 conn_max_size       =   100;/*连接数最大数量*/
    private int                 conn_min_size       =   10;/*连接数最小数量*/
    private boolean             run                 =   false;
    private ReentrantLock       conn_lock           =   new ReentrantLock();
    private Object              cmd_write_wait      =   new Object();
    private Object              cmd_recv_wait       =   new Object();
    private byte[]              cmd_recv            =   null;
    private byte[]              cmd_send            =   null;
    private Object              init_wait           =   new Object();
    private InetSocketAddress   serveraddress       =   null;
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
        this.client_data.setSessionCacheBufferSize(SESSION_READ_CACHE);
    }
    
    /**
     * 设置数据传送socket 读缓冲区大小
     * @param size
     * @return
     */
    public NodeSession setDataConnReadBufferSize(int size){
        this.client_data.setSessionCacheBufferSize(size);
        return this;
    }
    
    /**
     * 初始化
     * @param timeout 0 为不等待, 等待的毫秒数
     * @return 返回初始化的连接数 -1 表示连接服务器失败
     */
    public synchronized int init(long timeout){
        try {
            run = true;
            client_cmd = new SyncSocketClient();
            client_cmd.setRecvTimeOut(30 * 1000);
            if(client_cmd.connection(serveraddress, (int)timeout) == false){
                run = false;
                return -1;
            }
            new Thread(CmdConnection).start();
            byte[] initdata = send_cmd(new NetPack(NetPack.CMD_INIT, nodeid.getBytes()).Encode());
            if(initdata == null) {
                run = false;
                return -1;
            }
            NetPack recv = new NetPack(initdata);
            if(recv.type == NetPack.CMD_INIT){
                this.nodeid = new String(recv.body);
                if(Util.CheckNull(this.nodeid)) return 0;
                client_data.init();
                getSomeConnection(conn_min_size + 5);
                if(timeout > 0 && session_capacity + 1 < conn_min_size){
                    synchronized (init_wait) {
                        try {
                            init_wait.wait(timeout);
                        } catch (Exception e) {}
                    }
                }
                return session_capacity + 1;
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
        int nowmax = conn_max_size - session_capacity;
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
    
    public void sendMessage(Message msg){
        sendMessage(msg, 0);
    }
    
    public Message sendMessage(Message msg, long waitrestimeout){
        msg.from = nodeid;
        msg.msgid = Util.getUUID();
        Session session = null;
        synchronized (this) {
            if(session_capacity > 0){
                if(session_bound > session_capacity) session_bound = 0;
                session = sessions[session_bound];
                session_bound++;
            }
        }
        if(session != null){
            session.write(new NetPack(NetPack.CMD_MSG, msg.encode(charset)).Encode());
        }
        
        return null;
    }
    
    private void send_data(Session session, byte[] data){
        if(enableEncryption){
            data = Util.Encoded(data, user_en_key);
        }
        session.write(data);
    }
    
    
    private synchronized byte[] send_cmd(byte[] data){
        if(enableEncryption){
            data = Util.Encoded(data, user_en_key);
        }
        cmd_send = data;
        cmd_recv = null;
        synchronized (cmd_write_wait) {
            cmd_write_wait.notifyAll();
        }
        synchronized (cmd_recv_wait) {
            try {
                if(cmd_recv == null){
                    cmd_recv_wait.wait(20000);
                }
            } catch (InterruptedException e) {
                cmd_recv = null;
            }
        }
        return cmd_recv;
    }
    
    /**
     * 此方法不要在初始化后调用 无效
     * @param size
     */
    public void setConnectionMaxSize(int size){
        if(session_capacity > 0) return;
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
        return session_capacity > 0;
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
        run = false;
        synchronized (cmd_write_wait) {
            cmd_write_wait.notifyAll();   
        }
        synchronized (cmd_recv_wait) {
            cmd_recv_wait.notifyAll();
        }
        subs.clear();
        for(int i = 0; i < session_capacity; i++){
            if(sessions[i] != null){
                sessions[i].closeNow();
                sessions[i] = null;
            }
        }
        session_capacity = -1;
        session_bound = 0;
        client_data.close();
        client_cmd.close();
    }

    /**
     * @param action
     */
    public synchronized boolean addSubscribe(String action) {
        if(Util.CheckNull(action)) return false;
        byte[] res = send_cmd(new NetPack(NetPack.CMD_SUBS_MSG, action.getBytes(charset)).Encode());
        if(res == null || res.length < 2) return false; 
        NetPack pack = new NetPack(res);
        if(pack.type == NetPack.CMD_SUBS_MSG && subs.contains(action) == false){
            subs.add(action);
        }
        return pack.type == NetPack.CMD_SUBS_MSG;
    }
    
    /**
     * @param action
     */
    public synchronized boolean removeSubscribe(String action) {
        if(Util.CheckNull(action)) return false;
        byte[] res = send_cmd(new NetPack(NetPack.CMD_UNSUBS_MSG, action.getBytes(charset)).Encode());
        if(res == null || res.length < 2) return false; 
        NetPack pack = new NetPack(res);
        if(pack.type == NetPack.CMD_UNSUBS_MSG && subs.contains(action)){
            subs.remove(action);
        }
        return pack.type == NetPack.CMD_UNSUBS_MSG ;
    }
    
    private Runnable CmdConnection  = new Runnable() {
        
        @Override
        public void run() {
            while(run){
                try {
                    synchronized (cmd_write_wait) {
                        cmd_write_wait.wait(1000 * 60 * 3);//3分钟
                    }
                    if(cmd_send != null){
                        cmd_recv = client_cmd.send(cmd_send);
                        cmd_send = null;
                        synchronized (cmd_recv_wait) {
                            cmd_recv_wait.notifyAll();
                        }
                    }
                } catch (InterruptedException e) {
                    /*超时则发送心跳*/
                    send_cmd(new NetPack(NetPack.CMD_KEEP, null).Encode());
                }
            }
        }
    };
    
    private class DataHandler extends IoHandler{
        
        @Override
        public void onConnection(Session session) throws Exception {
            send_data(session, new NetPack(NetPack.CMD_DATA_INIT, nodeid.getBytes(charset)).Encode());
        }
        
        @Override
        public void onClose(Session session) throws Exception {
            Object obj = session.getAttr(SESSION_IS_INIT);
            System.out.println("client session close " + session);
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
                            if(session_capacity >= conn_min_size - 1){
                                synchronized (init_wait) {
                                    init_wait.notifyAll();
                                }
                            }
                        } catch (Exception e) {
                            
                        }finally{
                            conn_lock.unlock();
                        }
                        break;
                    }
                    case NetPack.CMD_MSG:
                    {
                        Message m = new Message(pack.body, charset);
                        switch (m.type) {
                            case Message.MTYPE_MESSAGE:
                            {
                                if(m.needReceipt){
                                    session.write(new NetPack(NetPack.CMD_RES, new MessageResult(true, m.msgid, m.to).Encode(charset)).Encode());
                                }
                                handler.onRecvMessage(NodeSession.this, m);
                                break;
                            }
                            case Message.MTYPE_BROADCAT:
                                handler.onRecvBroadcast(NodeSession.this, m);
                                break;
                            case Message.MTYPE_SUBSCRIBE:
                                handler.onRecvSubscribe(NodeSession.this, m.to, m);
                                break;
                        }
                        break;
                    }
                    case NetPack.CMD_RES:
                    {
                        handler.onRecvMessageResponse(NodeSession.this, new MessageResult(pack.body, charset));
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

}
