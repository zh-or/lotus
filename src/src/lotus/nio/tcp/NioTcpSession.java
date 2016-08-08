package lotus.nio.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import lotus.nio.IoEventRunnable;
import lotus.nio.IoEventRunnable.IoEventType;
import lotus.nio.NioContext;
import lotus.nio.Session;

public class NioTcpSession extends Session{
	private SocketChannel                  channel;
	private SelectionKey                   key;
	private LinkedBlockingQueue<Object>    qwrite;
	private volatile boolean               sentclose  = false;
	private volatile boolean               closed     = false;
	private SocketAddress                  remoteaddr;
	private NioTcpIoProcess                ioprocess;
	
	public NioTcpSession(NioContext context, SocketChannel channel, NioTcpIoProcess ioprocess, long id) {
		super(context, id);
		this.channel = channel;
		this.qwrite = new LinkedBlockingQueue<Object>();
		this.ioprocess = ioprocess;
		try {
            remoteaddr = channel.getRemoteAddress();
        } catch (IOException e) {}
	}
	
	public void setKey(SelectionKey key){
	    this.key = key;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return remoteaddr;
	}

	@Override
	public void write(Object data) {
        qwrite.add(data);
        if(key == null || !key.isValid()){/*没有准备好?*/
            context.ExecuteEvent(new IoEventRunnable(new Exception("session is not valid"), IoEventType.SESSION_EXCEPTION, this, context));
            return;
        }
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);/*注册写事件*/
        key.selector().wakeup();
	}
	
	public void write(Object data, boolean sentclose){
		write(data);
		this.sentclose = sentclose;
	}
	
	public Object poolMessage(){
	    Object obj = qwrite.poll();
	    
	    return obj;
	}
	
	public SocketChannel getChannel(){
		return channel;
	}
	
	public boolean isSentClose(){
		return sentclose;
	}
	
	@Override
	public void closeNow() {
	    if(closed) return;
	    closed = true;
        super.closeNow();
		try {
            channel.close();
            key.cancel();
            ioprocess.cancelSession(this);
        } catch (IOException e) {}
	}

	@Override
	public void closeOnFlush() {
		sentclose = true;
	}

}
