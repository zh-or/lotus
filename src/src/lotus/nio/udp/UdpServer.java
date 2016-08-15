package lotus.nio.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import lotus.nio.NioContext;

public class UdpServer extends NioContext{
    private DatagramChannel   dc            =   null;
    
    public UdpServer() {
        super();
    }
    
    public void bind(InetSocketAddress addr) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(addr);
        dc.configureBlocking(false);
    }
    
    public void unbind() {
        try {
            dc.close();
        } catch (IOException e) {}
    }
    
}
