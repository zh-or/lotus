package lotus.test;

import java.io.IOException;
import java.net.SocketAddress;

import lotus.cluster.MessageListenner;
import lotus.cluster.Node;
import lotus.nio.Session;
import lotus.nio.udp.UdpSession;

public class Test_cluster_service extends MessageListenner{

    
    public static void main(String[] args) throws IOException {
        Node node = new Node(null, "123", 10);
        Session s1 = new UdpSession(null, 1);
        Session s2 = new UdpSession(null, 2);
        Session s3 = new UdpSession(null, 3);
        Session s4 = new UdpSession(null, 4);
        Session s5 = new UdpSession(null, 5);
        Session s6 = new UdpSession(null, 6);
        node.AddDataSession(s1);
        node.AddDataSession(s2);
        node.AddDataSession(s3);
        node.AddDataSession(s4);
        node.AddDataSession(s5);
        node.AddDataSession(s6);
        
        
        node.removeDataSession(s4);
        node.removeDataSession(s2);
        
    }
    
}
