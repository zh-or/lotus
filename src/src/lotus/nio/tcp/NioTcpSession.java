package lotus.nio.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import lotus.nio.NioContext;
import lotus.nio.Session;

public class NioTcpSession extends Session {
	private SocketChannel                  channel;
	private SelectionKey                   key;
	private LinkedBlockingQueue<Object>    qwrite;
	private volatile boolean 			   sentClose = false;
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
	    synchronized(msglock) {
	        qwrite.add(data);
	        if(key == null || !key.isValid()) {//没有准备好? 可能被关闭了
				System.out.println(this.toString() + " :: 发送消息时发现连接不可用了------------------------------------");
	            if(!channel.isOpen()) {

					closeNow();
					return;
				}
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
	public int writeToChannel(ByteBuffer buf) throws IOException {
	    return channel.write(buf);
	}

	/**
	 * 直接从channel内读取buffer
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public int readFromChannel(ByteBuffer buf) throws IOException {
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

	public synchronized void write(Object data, boolean sentClose) {
		write(data);
		this.sentClose = sentClose;
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
     * @param checkCallback 此回调用来判断收到的消息是否是当前需要的返回
     * @return
     */
	public Object writeAndWaitForMessage(Object data, int timeout, MessageCheckCallback checkCallback) {
		checkCallback.setSendMsg(data);
	    write(data);
	    return waitForMessage(timeout, checkCallback);
	}

	/**
	 * 等待一条消息
	 * @param timeout
	 * @return
	 */
	public Object waitForMessage(int timeout, MessageCheckCallback checkCallback) {
	    this.msgcheckcallback = checkCallback;
	    packWait(timeout);
        Object tMsg = getPack();
        setPack(null);
        return tMsg;
	}


	public Object poolMessage() {
	    Object obj = qwrite.poll();

	    return obj;
	}

	@Override
	public int getWriteMessageSize() {
	    return qwrite.size();
	}

	public SocketChannel getChannel(){
		return channel;
	}

	public boolean isBlocking() {
	    return channel.isBlocking();
	}

	public synchronized boolean isSentClose() {
		return sentClose;
	}

	@Override
	public synchronized void closeNow() {
		if(closed) {
			//调用关闭后, selector 还会再次触发一个读事件, 读取时会返回-1
			System.out.println("bug");
			return;
		}
		if(key != null) {
			try {
				key.channel().close();
			} catch (IOException e) {}
			try {
				key.cancel();
				key.selector().wakeup();
			} catch (Exception e) {}
		}
		//ioprocess.cancelKey(key);
        super.closeNow();
	}

	@Override
	public synchronized void closeOnFlush() {
		sentClose = true;
	}

}
