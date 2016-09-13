package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.cluster.MessageListenner;
import lotus.cluster.service.ClusterService;

public class Test_cluster_service extends MessageListenner{

    
    public static void main(String[] args) throws IOException {
        ClusterService service = new ClusterService();
        service.start(new InetSocketAddress(5000));
        System.out.println("服务器启动完成...");
    }
    
}
