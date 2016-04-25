package lotus.cluster.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lotus.cluster.Message;
import lotus.cluster.MessageFactory;
import lotus.cluster.MessageListenner;
import lotus.cluster.NetPack;
import lotus.cluster.Node;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.socket.server.SocketServer;
import lotus.util.Util;

public class ClusterService {
    
    private SocketServer    server						=	null;
    private String          host                        =   "0.0.0.0";
    private int             port                        =   5000;
    private int             exthreadtotal               =   10;
    private int             read_buffer_size            =   2048;
    private int             idletime                    =   60;
    private int             buffer_list_maxsize         =   1024;
    private int             socket_timeout              =   50000;
    private boolean         enableEncryption            =   false;
    private String          en_key                      =   DEF_ENCRYPTION_KEY;
    private MessageFactory  msgfactory                  =   null;
    
    private static String   NODE_ID                     =   "node-id";
    private static String   KEEP_TIME                   =   "last-keep-time";
    private static String   CONN_TIME                   =   "commection-time";
    private static String   ENCRYPTION_KEY              =   "encryption-key";
    private static String   DEF_ENCRYPTION_KEY          =   "lotus-cluster-key";
    private static String   LASAT_ACTIVE                =   "session-last-active";
    private static Charset  DEF_CHARSET                 =   Charset.forName("utf-8");
    
    
    private MessageListenner listenner                  =   null;
    
    
    private ConcurrentHashMap<String, Node>               nodes             =   null;/* nodeid - > node */
    private ConcurrentHashMap<String, ArrayList<String>>  subscribeActions  =   null;/* type -> nodeids */
    private final Object                                  subactions_lock   =   new Object();
    
    public ClusterService(String host, int port) {
        this.host = host;
        this.port = port;
        this.listenner = new MessageListenner() {};
        this.nodes = new ConcurrentHashMap<String, Node>();
        this.subscribeActions = new ConcurrentHashMap<String, ArrayList<String>>();
        this.msgfactory = MessageFactory.getInstance();
    }
    
    public void setMessageCharset(String charsetName){
        DEF_CHARSET = Charset.forName(charsetName);
    }
    
    public void setMessageListenner(MessageListenner listenner){
        this.listenner = listenner;
    }
    
    public void setEnableEncryption(boolean isenable, String key){
        this.enableEncryption = isenable;
        this.en_key = key;
        if(Util.CheckNull(key)){
            this.en_key = DEF_ENCRYPTION_KEY;
        }
    }
    
    public boolean IsEnableEncryption(){
        return enableEncryption;
    }
    
    public void pushMessage(Node node, Message msg){
        
    }
    
    public void removeNode(String nodeid){
        
    }
    
    
    public void start() throws IOException{
        
        server = new SocketServer(host,
                port,
                exthreadtotal,
                read_buffer_size,
                idletime,
                buffer_list_maxsize,
                socket_timeout,
                new ExIoHandler());
        server.start();
    }
    
    public void stop(){
        server.stop();
    }
    
    
    private void sendPack(Session session, byte[] data){
        
        if(enableEncryption){
            data = Util.Encoded(data, session.getAttr(ENCRYPTION_KEY, DEF_ENCRYPTION_KEY) + "");
        }
        SocketServer.send(session, data);
    }
    
    private void sessionAddSubscribe(Node node, String action){
        node.addSubscribe(action);
        ArrayList<String> nodes = subscribeActions.get(action);
        if(nodes == null){
            synchronized (subactions_lock) {
                nodes = subscribeActions.get(action);
                if(nodes == null){/*还是null*/
                    nodes = new ArrayList<String>();
                    nodes.add(node.getNodeId());
                    subscribeActions.put(action, nodes);                    
                }
            }
        }else{
            synchronized (nodes) {
                nodes.add(node.getNodeId());
            }
        }
    }
    
    private void sessionRemoveSubscribe(Node node, String action){
        node.removeSubscribe(action);
        ArrayList<String> nodes = subscribeActions.get(action);
        if(nodes != null){
            synchronized (nodes) {
                nodes.remove(action);
            }
        }
        /*没有注册上? 那就不管了, 出现了这个问题了再说吧. :(*/
    }
    
    private Node sessionCheck(Session session){
        String nodeid = session.getAttr(NODE_ID, "") + "";
        if(Util.CheckNull(nodeid)){
            return null;
        }
        Node node = nodes.get(nodeid);
        /*做些检查*/
        return node;
    }
    
    
    private class ExIoHandler extends IoHandler{
        
        @Override
        public void onConnection(Session session) throws Exception {
            long t = System.currentTimeMillis();
            session.setAttr(ENCRYPTION_KEY, en_key);
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
            
            NetPack pack = new NetPack(data);
            byte packtype = pack.type;
            try {
                switch (packtype) {
                    case NetPack.CMD_MSG:/*普通消息消息*/
                    {
                        Message tmsg = MessageFactory.decode(pack.body, DEF_CHARSET);
                        byte mtype = tmsg.type;
                        switch (mtype) {
                            case Message.MTYPE_BROADCAT:
                            {
                                Node node = sessionCheck(session);
                                if(node != null && listenner.onRecvBroadcast(ClusterService.this, node, tmsg)){
                                    Iterator<Entry<String, Node>> it = nodes.entrySet().iterator();
                                    Entry<String, Node> item;
                                    while(it.hasNext()){
                                        item = it.next();
                                        node = item.getValue();
                                        sendPack(node.getSession(), data);
                                    }
                                }
                            }
                                break;
                            case Message.MTYPE_MESSAGE:
                            {
                                Node node = nodes.get(tmsg.to);
                                if(node != null){
                                    sendPack(node.getSession(), data);
                                    listenner.onRecvMessage(ClusterService.this, tmsg);
                                }else{/*直接返回失败*/
                                    
                                }
                            }
                                break;
                            case Message.MTYPE_SUBSCRIBE:
                            {
                                listenner.onRecvSubscribe(ClusterService.this, sessionCheck(session), tmsg);
                                ArrayList<String> nodeids = subscribeActions.get(tmsg.to);
                                if(nodeids != null){
                                    Node node;
                                    for(String nodeid : nodeids){
                                        node = nodes.get(nodeid);
                                        if(node != null){
                                            sendPack(node.getSession(), data);
                                        }
                                    }
                                }
                            }
                                break;
                        }
                        msgfactory.destory(tmsg);
                    }
                        break;
                    case NetPack.CMD_RES:/*消息回执*/
                        
                        break;
                    case NetPack.CMD_INIT:/*初始化*/
                    {
                        String nodeid = new String (pack.body, DEF_CHARSET);
                        Node node = nodes.get(nodeid);/*是不是重复了?*/
                        if(node != null){
                            Session oldsession = node.getSession();
                            if(oldsession != session){
                                oldsession.closeNow();
                            }
                            node.updateSession(session);
                        }else{
                            node = new Node(session, nodeid);
                        }
                        nodes.put(nodeid, node);
                        sendPack(session, data);
                        session.setAttr(ENCRYPTION_KEY, nodeid);
                        session.setAttr(NODE_ID, nodeid);
                        listenner.onNodeInit(ClusterService.this, node);
                    }
                        break;
                    case NetPack.CMD_SUBS_MSG:/*订阅消息*/
                    {
                        Node node = sessionCheck(session);
                        String action = new String(pack.body, DEF_CHARSET);
                        if(node != null && !Util.CheckNull(action)){
                            sessionAddSubscribe(node, action);
                            sendPack(session, data);
                            listenner.onSubscribeMessage(ClusterService.this, node, action);
                        }else{
                            session.closeNow();
                        }
                    }
                        break;
                    case NetPack.CMD_UNSUBS_MSG:/*取消订阅消息*/
                    {
                        Node node = sessionCheck(session);
                        String action = new String(pack.body, DEF_CHARSET);
                        if(node != null && !Util.CheckNull(action)){
                            sessionRemoveSubscribe(node, action);
                            sendPack(session, data);
                            listenner.onUnSubscribeMessage(ClusterService.this, node, action);
                        }else{
                            session.closeNow();
                        }
                    }
                        break;
                    case NetPack.CMD_KEEP:/*心跳*/
                        /*不需要处理*/
                        break;
                    default:
                        session.closeNow();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onSentMessage(Session session, Object msg) throws Exception {
            listenner.onMessageSent(ClusterService.this, (ByteBuffer) msg);
        }
        
        @Override
        public void onIdle(Session session) throws Exception {
            String nodeid = session.getAttr(NODE_ID, "") + "";
            if(!Util.CheckNull(nodeid)){
                
            }else{
                session.closeNow();/*滚蛋去*/
            }
        }
        
        @Override
        public void onClose(Session session) throws Exception {
            String nodeid = session.getAttr(NODE_ID, "") + "";
            if(!Util.CheckNull(nodeid)){
                /*取消所有广播*/
                Node node = nodes.get(nodeid);
                if(node != null && node.getSession() == session){
                    ArrayList<String> subactions = node.getSubscribeActions();
                    for(String action : subactions){
                        sessionRemoveSubscribe(node, action);
                    }
                }
            }
        }
        
    }
}
