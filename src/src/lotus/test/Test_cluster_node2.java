package lotus.test;

import java.net.InetSocketAddress;

import lotus.cluster.Message;
import lotus.cluster.MessageResult;
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
        node.setDataConnReadBufferSize(1024);
        node.setHandler(new MessageHandler() {
            @Override
            public void onRecvBroadcast(NodeSession node, Message msg) {
                System.out.println("节点收到广播:" + msg.toString());
            }
            @Override
            public void onRecvMessage(NodeSession node, Message msg) {
                System.out.println("节点收到消息:" + msg.toString());
            }
            @Override
            public void onRecvMessageResponse(NodeSession node, MessageResult msgres) {
                System.out.println("节点收到消息回执:" + msgres.toString());
            }
            @Override
            public void onRecvSubscribe(NodeSession node, String action, Message msg) {
                System.out.println("节点收到订阅消息:" + action + "," + msg.toString());
            }
        });
        log.info("init..");
        int conn = node.init(10000);
        log.info("init:%d", conn);
        if(conn <= 0){
            log.info("初始化失败...");
            Util.SLEEP(3000);
            System.exit(1);
        }
        String action = "helloworld";
        System.out.println("add:" + node.addSubscribe("helloworld"));;
        node.sendMessage(new Message(Message.MTYPE_BROADCAT, action, null, "head", null));
        node.sendMessage(new Message(Message.MTYPE_MESSAGE, node.getNodeId(), null, "head", null));
        node.sendMessage(new Message(Message.MTYPE_SUBSCRIBE, action, null, "head", null));
        
        Util.SLEEP(5000);
        System.out.println("remove:" + node.removeSubscribe("helloworld"));;
        node.close();
        Util.SLEEP(20000);
        System.exit(0);
    }
    
}
