package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.cluster.service.ClusterService;
import lotus.cluster.service.MessageListenner;
import lotus.cluster.service.Node;

public class Test_cluster_service extends MessageListenner{

    
    public static void main(String[] args) throws IOException {
        ClusterService service = new ClusterService();
        service.setMessageListenner(new MessageListenner() {
            @Override
            public void onNodeInit(ClusterService service, Node node) {
                System.out.println("节点初始化:" + node.getNodeId());
            }
            
            @Override
            public void onNodeConnectionsChanged(ClusterService service, Node node) {
                System.out.println("节点连接数改变:" + node.size());
            }
        });
        service.start(new InetSocketAddress(5000));
        System.out.println("服务器启动完成...");
    }
    
}
