package lotus.cluster.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import lotus.cluster.MessageListenner;
import lotus.cluster.NetPack;
import lotus.cluster.Node;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.socket.server.SocketServer;
import lotus.util.Util;

public class ClusterService {
    
    private SocketServer    server						=	null;
    private int             exthreadtotal               =   0;/*都在io线程里面处理了吧*/
    private int             read_buffer_size            =   2048;
    private int             idletime                    =   60;
    private int             buffer_list_size            =   1024;
    private int             socket_timeout              =   50000;
    
    private int             conn_max_size               =   100;/*连接数最大数量*/
    private int             conn_low_size               =   10;/*连接数最小数量*/
    
    private boolean         enableEncryption            =   false;
    private String          use_encryption_key          =   DEF_ENCRYPTION_KEY;//当前引用的加密密码
    
    
    private static String   NODE_ID                     =   "node-id";
    private static String   KEEP_TIME                   =   "last-keep-time";
    private static String   CONN_TIME                   =   "connection-time";
    private static String   ENCRYPTION_KEY              =   "encryption-key";
    private static String   DEF_ENCRYPTION_KEY          =   "lotus-cluster-key";
    private static String   LASAT_ACTIVE                =   "session-last-active";
    private static Charset  DEF_CHARSET                 =   Charset.forName("utf-8");
    
    
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
        DEF_CHARSET = Charset.forName(charsetName);
        return this;
    }
    
    public ClusterService setMessageListenner(MessageListenner listenner){
        this.listenner = listenner;
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
            
            NetPack pack = new NetPack(data);
            byte packtype = pack.type;
            try {
               
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
                listenner.onNodeUnInit(ClusterService.this, node);
                
            }
        }
        
    }
}
