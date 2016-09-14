package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.cluster.service.ClusterService;
import lotus.cluster.service.MessageListenner;
import lotus.cluster.service.Node;

public class Test_cluster_service extends MessageListenner{

    
    public static void main(String[] args) throws IOException {
        ClusterService service = new ClusterService();
        service.setDataConnReadBufferSize(1024);
        service.setMessageListenner(new MessageListenner() {
            @Override
            public void onNodeInit(ClusterService service, Node node) {
                System.out.println("节点初始化:" + node.getNodeId());
            }
            
            @Override
            public void onNodeUnInit(ClusterService service, Node node) {
                System.out.println("节点取消初始化:" + node.getNodeId());
            }
            
            @Override
            public void onNodeConnectionsChanged(ClusterService service, Node node) {
                
            }
            
            @Override
            public void onRegSubscribeMessage(ClusterService service, Node node, String action) {
                System.out.println("节点订阅消息:" + node.getNodeId() + "," + action);
            }
            @Override
            public void onUnRegSubscribeMessage(ClusterService service, Node node, String action) {
                System.out.println("节点取消订阅消息:" + node.getNodeId() + "," + action);
            }
        });
        service.start(new InetSocketAddress(5000));
        System.out.println("服务器启动完成...");
    }
    
}
