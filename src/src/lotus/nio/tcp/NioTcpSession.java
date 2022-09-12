package lotus.nio.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
	private MessageCheckCallback           msgcheckcallback = null;
	
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
	
	public boolean callCheckMessageCallback(Object recvmsg){
	    return msgcheckcallback.thatsRight(recvmsg);
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
	
	/**
	 * 直接把buffer写入channel
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public int write(ByteBuffer buf) throws IOException {
	    return channel.write(buf);
	}
	
	/**
	 * 直接从channel内读取buffer
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public int read(ByteBuffer buf) throws IOException {
	    return channel.read(buf);
	}
	
	public void addInterestOps(int value){
	    key.interestOps(key.interestOps() | value);
	}

	public void removeInterestOps(int value){
	    key.interestOps(key.interestOps() & (~value));
	}
	
	public void removeAllOpts() {
	    removeAttr(SelectionKey.OP_ACCEPT);
	    removeAttr(SelectionKey.OP_CONNECT);
	    removeAttr(SelectionKey.OP_READ);
	    removeAttr(SelectionKey.OP_WRITE);
	}
	
	public Object getMessageLock(){
	    return msglock;
	}
	
	public void write(Object data, boolean sentclose){
		write(data);
		this.sentclose = sentclose;
	}
	
	/**
     * 发送并等待一条消息
     * @param data
     * @param timeout
     * @return
     */
    public Object writeAndWaitForMessage(Object data, int timeout){
        return writeAndWaitForMessage(data, timeout, new MessageCheckCallback() {});
    }
	
    /**
     * 发送并等待一条消息
     * @param data
     * @param timeout
     * @param checkcallback 此回调用来判断收到的消息是否是当前需要的返回
     * @return
     */
	public Object writeAndWaitForMessage(Object data, int timeout, MessageCheckCallback checkcallback){
	    checkcallback.setSendMsg(data);
	    write(data);
	    return waitForMessage(timeout, checkcallback);
	}
	
	/**
	 * 等待一条消息
	 * @param timeout
	 * @return
	 */
	public Object waitForMessage(int timeout, MessageCheckCallback checkcallback){
	    this.msgcheckcallback = checkcallback;
	    _wait(timeout);
        Object tmsg = get();
        set(null);
        return tmsg;
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
	
	public boolean isBlocking() {
	    return channel.isBlocking();
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
