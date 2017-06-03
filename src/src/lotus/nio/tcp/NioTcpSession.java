package lotus.nio.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import lotus.nio.NioContext;
import lotus.nio.Session;

public class NioTcpSession extends Session{
	private SocketChannel                  channel;
	private SelectionKey                   key;
	private LinkedBlockingQueue<Object>    qwrite;
	private volatile boolean               sentclose  = false;
	private NioTcpIoProcess                ioprocess;
	private Object                         msglock;
	
	public NioTcpSession(NioContext context, SocketChannel channel, NioTcpIoProcess ioprocess, long id) {
		super(context, id);
		this.channel = channel;
		this.qwrite = new LinkedBlockingQueue<Object>();
		this.msglock = new Object();
		this.ioprocess = ioprocess;
	}
	
	public void setKey(SelectionKey key){
	    this.key = key;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		try {
            return channel.getRemoteAddress();
        } catch (IOException e) {
        }
		return null;
	}

	@Override
	public SocketAddress getLocaAddress() {
	    try {
            return channel.getLocalAddress();
        } catch (IOException e) {}
	    return null;
	}
	
	@Override
	public void write(Object data) {
	    synchronized(msglock){
	        qwrite.add(data);
	        if(key == null || !key.isValid()){//没有准备好? 可能被关闭了
	            //context.ExecuteEvent(new IoEventRunnable(new Exception("session is not valid"), IoEventType.SESSION_EXCEPTION, this, context));
	            //context.ExecuteEvent(new IoEventRunnable(null, IoEventType.SESSION_CLOSE, this, context));
	            closeNow();
	            return;
	        }
	        
	        addInterestOps(SelectionKey.OP_WRITE);/*注册写事件*/
	        key.selector().wakeup();
	    }
	}
	
	public void addInterestOps(int value){
	    key.interestOps(key.interestOps() | value);
	}

	public void removeInterestOps(int value){
	    key.interestOps(key.interestOps() & (~value));
	}
	
	public Object getMessageLock(){
	    return msglock;
	}
	
	public void write(Object data, boolean sentclose){
		write(data);
		this.sentclose = sentclose;
	}
	
	public void writeAndWaitRecv(Object data, int timeout){
	    write(data);
	    _wait(timeout);
	}
	
	
	public Object poolMessage(){
	    Object obj = qwrite.poll();
	    
	    return obj;
	}
	
	@Override
	public int getWriteMessageSize(){
	    return qwrite.size();
	}
	
	public SocketChannel getChannel(){
		return channel;
	}
	
	public boolean isSentClose(){
		return sentclose;
	}
	
	@Override
	public synchronized void closeNow() {
	    if(closed) return;
        super.closeNow();
        ioprocess.cancelKey(key);

	}

	@Override
	public void closeOnFlush() {
		sentclose = true;
	}

}
