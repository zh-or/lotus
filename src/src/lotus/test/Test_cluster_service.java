package lotus.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import lotus.cluster.Message;
import lotus.cluster.MessageListenner;
import lotus.cluster.Node;
import lotus.cluster.service.ClusterService;
import lotus.log.Log;

public class Test_cluster_service extends MessageListenner{
    static ClusterService sercice;
    static Log log;
    static int recv = 0;
    static int send = 0;
    static Object lock_recv = new Object();
    static Object lock_send = new Object();
            
    
    public static void main(String[] args) throws IOException {
        sercice = new ClusterService("0.0.0.0", 5000);
        sercice.setMessageListenner(new Test_cluster_service());
        sercice.start();
        log = Log.getInstance();
        log.setProjectName("CMQ");
        log.info("service start soucess, port: %d", 5000);
        Timer t = new Timer();
        
        t.schedule(new TimerTask() {
            int tsend = 0;
            int trecv = 0;
            
            @Override
            public void run() {
                synchronized(lock_send){
                    tsend = send;
                    send = 0;
                }
                synchronized(lock_recv){
                    trecv = recv;
                    recv = 0;
                }
                
                log.debug("当前 1 秒钟收到 %d 条, 发送 %d 条", trecv, tsend);
            }
        }, 6000, 1000);
        
    }
    
    
    @Override
    public void onNodeInit(ClusterService service, Node node) {
        log.info("node init addr:%s nodeid:%s", node.getSession().getRemoteAddress() + "", node.getNodeId());
    }
    
    @Override
    public boolean onRecvBroadcast(ClusterService service, Node node, Message msg) {
        synchronized (lock_recv) {
            recv ++;
        }
        //        log.info("recv broadcast from:%s msg:%s", node.getSession().getRemoteAddress(), msg.toString());
        return false;
    }
    
    @Override
    public void onRecvMessage(ClusterService service, Message msg) {
        synchronized (lock_recv) {
            recv ++;
        }
      //  log.info("recv message from:%s to:%s", msg.from, msg.to);
    }
    
    @Override
    public void onRecvSubscribe(ClusterService service, Node node, Message msg) {
        synchronized (lock_recv) {
            recv ++;
        }
     //   log.info("recv subscribe from:%s to:%s", msg.from, msg.to);
    }
    
    @Override
    public void onSubscribeMessage(ClusterService service, Node node, String action) {
        log.info("recv subscribe nodeid:%s action:%s", node.getNodeId(), action);
    }
    
    @Override
    public void onUnSubscribeMessage(ClusterService service, Node node, String action) {
        log.info("recv unsubscribe message nodeid:%s action:%s", node.getNodeId(), action);
    }
    
    
    @Override
    public void onMessageSent(ClusterService service, ByteBuffer buff) {
        synchronized(lock_send){
            send ++;
        }
    }
}
