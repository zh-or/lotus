package lotus.nio.udp;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.LinkedBlockingQueue;

import lotus.nio.NioContext;
import lotus.nio.Session;


public class UdpSession extends Session{

    private String addresss;
    private int port;
    private LinkedBlockingQueue<Object> qwrite;
    private DatagramChannel channel;
    



    public UdpSession(NioContext context, long id) {
        super(context, id);
        this.qwrite = new LinkedBlockingQueue<Object>();
    }

    public void  setKey(DatagramChannel channel, String addresss, int port) {
        this.addresss = addresss;
        this.port = port;
        this.channel = channel;
    }
	

    @Override
    public SocketAddress getRemoteAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(Object obj){
        qwrite.add(obj);
    }
    

    @Override
    public void closeOnFlush() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getWriteMessageSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public SocketAddress getLocaAddress() {
        // TODO Auto-generated method stub
        return null;
    }
	
}
