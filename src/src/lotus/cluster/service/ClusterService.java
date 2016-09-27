package lotus.cluster.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lotus.cluster.Message;
import lotus.cluster.MessageResult;
import lotus.cluster.NetPack;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.socket.server.SocketServer;
import lotus.util.Util;

public class ClusterService {
    
    private SocketServer    server						=	null;
    private int             exthreadtotal               =   10;/*都在io线程里面处理了吧*/
    private int             read_buffer_size            =   2048;
    private int             idletime                    =   60 * 3;
    private int             buffer_list_size            =   1024;
    private int             socket_timeout              =   50000;
    
    private int             conn_max_size               =   100;/*连接数最大数量*/
    
    private boolean         enableEncryption            =   false;
    private String          use_encryption_key          =   DEF_ENCRYPTION_KEY;//当前引用的加密密码
    
    
    private static String   NODE_ID                     =   "node-id";
    private static String   SESSION_TYPE                =   "session-type";
    private static String   SESSION_TYPE_DATA           =   "session-type-data";
    private static String   SESSION_TYPE_CMD            =   "session-type-cmd";
    private static String   KEEP_TIME                   =   "last-keep-time";
    private static String   CONN_TIME                   =   "connection-time";
    private static String   ENCRYPTION_KEY              =   "encryption-key";
    private static String   DEF_ENCRYPTION_KEY          =   "lotus-cluster-key";
    private static String   LASAT_ACTIVE                =   "session-last-active";
    private static Charset  charset                     =   Charset.forName("utf-8");
    
    
    private MessageListenner listenner                  =   null;
    
    
    private ConcurrentHashMap<String, Node>               nodes             =   null;/* nodeid - > node */
    private ConcurrentHashMap<String, ArrayList<String>>  subscribeActions  =   null;/* type -> nodeids */
    private final Object                                  subactions_lock   =   new Object();
    
    public ClusterService() {

        this.listenner = new MessageListenner() {};
        this.nodes = new ConcurrentHashMap<String, Node>();
        this.subscribeActions = new ConcurrentHashMap<String, ArrayList<String>>();
        
    }
    
    public ClusterService setMessageCharset(String charsetName){
        charset = Charset.forName(charsetName);
        return this;
    }
    
    public ClusterService setMessageListenner(MessageListenner listenner){
        this.listenner = listenner;
        return this;
    }
    
    public ClusterService setMaxDataConnection(int count){
        this.conn_max_size = count;
        return this;
    }
    
    /**
     * 消息加密开关
     * @param isenable
     * @param key 如果为空则表示使用默认密码
     * @return
     */
    public ClusterService setEnableEncryption(boolean isenable, String key){
        this.enableEncryption = isenable;
        this.use_encryption_key = key;
        if(Util.CheckNull(key)){
            this.use_encryption_key = DEF_ENCRYPTION_KEY;
        }
        return this;
    }
    
    public boolean IsEnableEncryption(){
        return enableEncryption;
    }
    
    public ClusterService setDataConnReadBufferSize(int size){
        this.read_buffer_size = size;
        return this;
    }
    
    public ClusterService start(InetSocketAddress addr) throws IOException{
        
        server = new SocketServer(addr);
        server.setEventThreadPoolSize(exthreadtotal);
        server.setReadbuffsize(read_buffer_size);
        server.setReadBufferCacheListSize(buffer_list_size);
        server.setIdletime(idletime);
        server.setSockettimeout(socket_timeout);
        
        server.setHandler(new ExIoHandler());
        server.start();
        
        return this;
    }
    
    public void stop(){
        server.stop();
    }
    
    public void sendMessage(Message msg){
        
    }
    
    public void sendBrodcast(String head, byte[] body){
        Message msg = new Message(Message.MTYPE_BROADCAST, null, Util.getUUID(), head, body);
        sendMessage(msg);
    }
    
    public void sendSubMessage(String action, String head, byte[] body){
        Message msg = new Message(Message.MTYPE_SUBSCRIBE, action, Util.getUUID(), head, body);
        sendMessage(msg);
    }
    
    private class ExIoHandler extends IoHandler{
        
        @Override
        public void onConnection(Session session) throws Exception {
            long t = System.currentTimeMillis();
            session.setAttr(ENCRYPTION_KEY, use_encryption_key);
            session.setAttr(CONN_TIME, t);
            session.setAttr(KEEP_TIME, t);
            session.setAttr(LASAT_ACTIVE, t);
            session.setAttr(NODE_ID, "");
        }
        
        @Override
        public void onRecvMessage(Session session, Object msg) throws Exception {
            byte[] data = (byte[]) msg;
            if(data == null || data.length < 1){
                /*wtf*/
                return;
            }
            if(enableEncryption){
                data = Util.Decode(data, session.getAttr(ENCRYPTION_KEY, DEF_ENCRYPTION_KEY) + "");
            }
            // String session_type = session.getAttr(SESSION_TYPE) + "";
            NetPack pack = new NetPack(data);
            byte packtype = pack.type;
            try {
                switch (packtype) {
                    case NetPack.CMD_MSG:
                    {
                        Message m = new Message(pack.body, charset);
                        Node node_from = nodes.get(session.getAttr(NODE_ID));
                        Node node_to = nodes.get(m.to);
                        switch (m.type) {
                            case Message.MTYPE_MESSAGE:
                            {
                                if(node_to != null){
                                    listenner.onRecvMessage(ClusterService.this, m);
                                    Session datasession = node_to.getOneConnection();
                                    if(datasession != null){
                                        datasession.write(data);
                                    } 
                                }else{
                                    if(m.needReceipt){
                                        session.write(new NetPack(NetPack.CMD_RES, new MessageResult(false, m.msgid, m.from).Encode(charset)).Encode());
                                    }
                                }
                                break;
                            }
                            case Message.MTYPE_SUBSCRIBE:
                            {
                                String action = m.to;
                                listenner.onRecvSubscribe(ClusterService.this, node_from, m);
                                ArrayList<String> subsnodes = subscribeActions.get(action);
                                if(subsnodes != null && subsnodes.size() > 0){
                                    Node node = null;
                                    for(String nodeid : subsnodes){
                                        node = nodes.get(nodeid);
                                        if(node != null){
                                            node.getOneConnection().write(data);
                                        }
                                    }
                                }
                                break;
                            }
                            case Message.MTYPE_BROADCAST:
                            {
                                
                                if(listenner.onRecvBroadcast(ClusterService.this, node_from, m) == false){
                                    Iterator<Entry<String, Node>> tmp = nodes.entrySet().iterator();
                                    while(tmp.hasNext()){
                                        Entry<String, Node> item = tmp.next();
                                        Node node = item.getValue();
                                        Session s = node.getOneConnection();
                                        if(s != null){
                                            s.write(data);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case NetPack.CMD_DATA_INIT:
                    {
                        String nodeid = new String(pack.body, charset);
                        Node node = nodes.get(nodeid);
                        if(node != null){
                            session.setAttr(SESSION_TYPE, SESSION_TYPE_DATA);
                            session.setAttr(NODE_ID, nodeid);
                            session.write(data);
                            node.AddDataSession(session);
                            listenner.onNodeConnectionsChanged(ClusterService.this, node);
                        }else{
                            System.out.println("没有找到该nodeid的连接, 关闭之");
                            session.closeNow();
                        }
                        break;
                    }
                    case NetPack.CMD_INIT:
                    {
                        String nodeid = new String(pack.body, charset);
                        session.setAttr(NODE_ID, nodeid);
                        /*有可能这个链接被断开了*/
                        Node node = nodes.get(nodeid);
                        if(node == null){
                            node = new Node(session, nodeid, conn_max_size);
                        }else{
                            node.reSetCmdSession(session);
                        }
                        session.setAttr(SESSION_TYPE, SESSION_TYPE_CMD);
                        session.write(data);
                        nodes.put(nodeid, node);
                        listenner.onNodeInit(ClusterService.this, node);
                        break;
                    }
                    case NetPack.CMD_SUBS_MSG:
                    {
                        String nodeid = session.getAttr(NODE_ID) + "";
                        Node node = nodes.get(nodeid);
                        if(node != null){
                            String action = new String(pack.body, charset);
                            node.addSubs(action);
                            ArrayList<String> subs = subscribeActions.get(action);
                            if(subs == null){
                                synchronized (subactions_lock) {
                                    subs = new ArrayList<String>();
                                    subscribeActions.put(action, subs);
                                }
                            }
                            synchronized (subs) {
                                if(subs.contains(subs) == false){
                                    subs.add(nodeid);
                                }
                            }
                            session.write(data);
                            listenner.onRegSubscribeMessage(ClusterService.this, node, action);
                        }else{
                            session.closeNow();
                        }
                        
                        break;
                    }
                    case NetPack.CMD_UNSUBS_MSG:
                    {
                        String nodeid = session.getAttr(NODE_ID) + "";
                        Node node = nodes.get(nodeid);
                        if(node != null){
                            String action = new String(pack.body, charset);
                            node.removeSubs(action);
                            ArrayList<String> subs = subscribeActions.get(action);
                            if(subs != null){
                                synchronized (subs) {
                                    subs.remove(nodeid);
                                }
                            }
                            session.write(data);
                            listenner.onUnRegSubscribeMessage(ClusterService.this, node, action);
                        }else{
                            session.closeNow();
                        }
                        break;
                    }
                    
                    
                    default:
                        /*未知的数据包*/
                        break;
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onSentMessage(Session session, Object msg) throws Exception {
            listenner.onMessageSent(ClusterService.this, msg);
        }
        
        @Override
        public void onIdle(Session session) throws Exception {
            String nodeid = session.getAttr(NODE_ID, "") + "";
            String session_type = session.getAttr(SESSION_TYPE) + "";
            if(Util.CheckNull(nodeid) == false && Util.CheckNull(session_type) == false){
                
            }else{
                System.out.println("空闲了->" + session.toString() + "," + session.getCreateTime() +"/" + System.currentTimeMillis() +", 关闭之");
                session.closeNow();
            }
        }
        
        @Override
        public void onClose(Session session) throws Exception {
            System.out.println("server session close:" + session);
            String nodeid = session.getAttr(NODE_ID, "") + "";
            String session_type = session.getAttr(SESSION_TYPE) + "";
            if(!Util.CheckNull(nodeid)){

                Node node = nodes.get(nodeid);
                if(SESSION_TYPE_DATA.equals(session_type)){
                    node.removeDataSession(session);
                    listenner.onNodeConnectionsChanged(ClusterService.this, node);
                    if(node.size() < 0 && node.getCmdSession() == null){
                        nodes.remove(nodeid);
                        listenner.onNodeUnInit(ClusterService.this, node);
                    }
                }else if(SESSION_TYPE_CMD.equals(session_type)){
                    node.reSetCmdSession(null);
                    if(node.size() <= 0){
                        nodes.remove(nodeid);
                        listenner.onNodeUnInit(ClusterService.this, node);
                    }
                }
            }
        }
        
    }
}
