package or.lotus.core.nio.tcp;

import or.lotus.core.nio.LotusPooledByteBuffer;
import or.lotus.core.nio.support.PromiseWrap;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class NioTcpServer {
    private ServerSocketChannel serverSocketChannel;

    /** io读写线程数 = cpu total + 1 */
    private int ioThreadTotal = Runtime.getRuntime().availableProcessors() + 1;

    /** 事件线程池线程数 */
    private int eventThreadTotal = 0;

    /** 待发送消息队列最大大小 */
    private int waitSendMsgQueueSize = 500000;//默认五十万条

    private LotusPooledByteBuffer bufferPool;

    private LinkedBlockingQueue<PromiseWrap> messageQueue;


    public NioTcpServer() {

    }

    public NioTcpServer setIoThreadTotal(int ioThreadTotal) {
        this.ioThreadTotal = ioThreadTotal;
        return this;
    }

    public NioTcpServer setEventThreadTotal(int eventThreadTotal) {
        this.eventThreadTotal = eventThreadTotal;
        return this;
    }

    public void sendMessage(NioTcpSession session, Object msg) {

    }

    public void start() {
        messageQueue = new LinkedBlockingQueue<>(waitSendMsgQueueSize);
        if(bufferPool == null) {
            bufferPool = new LotusPooledByteBuffer(1024, 1024 * 4);
        }
    }

    public void bind(InetSocketAddress address) {

    }
}
