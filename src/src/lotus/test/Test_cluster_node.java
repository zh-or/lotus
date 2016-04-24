package lotus.test;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import lotus.cluster.Message;
import lotus.cluster.MessageFactory;
import lotus.cluster.node.MessageHandler;
import lotus.cluster.node.NodeSession;
import lotus.log.Log;
import lotus.util.Util;

public class Test_cluster_node extends MessageHandler{
    static NodeSession node;
    static Log log;
    static MessageFactory msgfactory;
    static int recv = 0;
    static Object lock_recv = new Object();
    
    
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        if(args != null && args.length > 0){
            host = args[0];
        }
        log = Log.getInstance();
        msgfactory = MessageFactory.getInstance();
        log.setProjectName("node");
        Scanner in = new Scanner(System.in);
        int m = 1, total = 0;
        do{
            log.info("请选择模式: 1 发送, 2 接收:");
        }while((m = in.nextInt()) > 2 && (m <= 0));
        if(m == 1){
            log.info("请输入 1 秒钟要发送的信息条数:");
            total = in.nextInt();
        }
        
        String[] modes = new String[]{"", "发送", "接收"};
        log.info("当前模式:%s, 1 秒发送 %d 条消息", modes[m], total);
        in.close();
        node = new NodeSession(host, 5000, Util.getUUID());
        node.setHandler(new Test_cluster_node());
        log.info("init..");
        boolean isinit = node.init(10000);
        log.info("init:%s", isinit);
        if(!isinit){
            log.info("初始化失败...");
            Util.SLEEP(3000);
            System.exit(1);
        }
        log.info("订阅 fuck -> %s", node.addSubscribe("fuck"));
        
        
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            int trecv = 0;
            @Override
            public void run() {
                synchronized (lock_recv) {
                    trecv = recv;
                    recv = 0;
                }
                log.debug("1 秒钟收到 %d 条消息", trecv);
            }
        }, 1000, 1000);
        if(m == 1){
            long t1, t2;
            while(true){
                try {
                     t1 = System.currentTimeMillis();
                     
                     for(int i = 0; i < total; i++){
                         node.sendMessage(
                                 msgfactory.create(
                                         false,
                                         Message.MTYPE_SUBSCRIBE,
                                         "fuck",
                                         node.getNodeId(),
                                         Util.getUUID(),
                                         "MTYPE_SUBSCRIBE",
                                         new byte[]{1, 2, 3}));

                     }
                     t2 = System.currentTimeMillis();
                     long s = 1000 - (t2 - t1);
                     if(s > 0) Util.SLEEP(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Util.SLEEP(1000);
            }
        }else{
            
        }

    }
    
    @Override
    public void onClose(NodeSession node) {
        log.error("socket closed");
        Util.SLEEP(3000);
        System.exit(1);
    }
    
    @Override
    public void onRecvBroadcast(NodeSession node, Message msg) {
      //  log.info("recv broadcast from:%s msg:%s", msg.from, msg.toString());
        synchronized (lock_recv) {
            recv++;   
        }
    }
    
    @Override
    public void onRecvMessage(NodeSession node, Message msg) {
      //  log.info("recv from:%s msg:%s", msg.from, msg.toString());
        synchronized (lock_recv) {
            recv++;   
        }
    }
    
    @Override
    public void onRecvSubscribe(NodeSession node, Message msg) {
       // log.info("recv subscribe message from:%s msg:%s", msg.from, msg.toString());
        synchronized (lock_recv) {
            recv++;   
        }
    }
}
