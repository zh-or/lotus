package lotus.test;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import lotus.cluster.Message;
import lotus.cluster.node.MessageHandler;
import lotus.cluster.node.NodeSession;
import lotus.log.Log;
import lotus.util.Util;

public class Test_cluster_node2 extends MessageHandler{
    static NodeSession node;
    static Log log;
    
    
    public static void main(String[] args) throws Exception {

        log = Log.getInstance();
        log.setProjectName("node");

        node = new NodeSession(new InetSocketAddress(5000), Util.getUUID());
        node.setHandler(new Test_cluster_node2());
        node.setConnectionMinSize(30);
        log.info("init..");
        int conn = node.init(10000);
        log.info("init:%d", conn);
        if(conn <= 0){
            log.info("初始化失败...");
            Util.SLEEP(3000);
            System.exit(1);
        }
        
        //fuck?	

    }
    
}
