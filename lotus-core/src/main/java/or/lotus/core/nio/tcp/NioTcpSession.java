package or.lotus.core.nio.tcp;


import or.lotus.core.nio.IoProcess;
import or.lotus.core.nio.Session;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioTcpSession  extends Session {
	protected SocketChannel channel;
	protected SelectionKey key;
	protected boolean isCloseOnFlush = false;

	public NioTcpSession(NioTcpServer context, SocketChannel channel, IoProcess ioProcess) {
		super(context, ioProcess);
		this.channel = channel;
	}

	@Override
	public boolean write(Object data) {
		if(closed) {
			return false;
		}
		boolean r = super.write(data);
		ioProcess.addPendingTask(() -> {
			if(key.isValid()) {
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			}
		});
		key.selector().wakeup();
		return r;
	}

	protected Object pollMessage() {
		Object msg = waitSendMessageList.poll();

		return msg;
	}


	/** 会触发关闭事件 */
	@Override
	public void closeNow() {
		super.closeNow();
		if(key != null) {
			try {
				key.channel().close();
			} catch (IOException e) {}
			key.cancel();
			key.selector().wakeup();//清理关闭的 key
		}
	}

	/** 将在最后一条消息发送后执行 closeNow */
	public void closeOnFlush() {
		isCloseOnFlush = true;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		try {
            return (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
        }
		return null;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
	    try {
            return (InetSocketAddress) channel.getLocalAddress();
        } catch (IOException e) {}
	    return null;
	}

}
