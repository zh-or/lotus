package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.IoEventRunnable;
import or.lotus.core.nio.NioContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.locks.ReentrantLock;

public class NioUdpClient extends NioContext {

    protected NioUdpIoProcess[] ioProcess;
    private int ioProcessBound = 0;
    private final ReentrantLock boundLock = new ReentrantLock();

    public NioUdpClient() {
        this(1024 * 3, 4 * 1024, false);
    }

    public NioUdpClient(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer) {
        this(cacheBufferSize, bufferCapacity, useDirectBuffer, Runtime.getRuntime().availableProcessors() + 1);
    }

    public NioUdpClient(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer, int ioThreadTotal) {
        super(cacheBufferSize, bufferCapacity, useDirectBuffer, ioThreadTotal);
        if(bufferCapacity <= 0) {
            this.bufferCapacity = 65507;//udp 最大数据包大小
        }
    }

    @Override
    public void bind(InetSocketAddress address) throws IOException {
        throw new RuntimeException("如需单个链接绑定到指定端口请使用session的bind");
    }

    public NioUdpSession connection(InetSocketAddress remoteAddress) throws IOException {
        return connection(remoteAddress, new InetSocketAddress(0));
    }

    public NioUdpSession connection(InetSocketAddress remoteAddress, InetSocketAddress localAddress) throws IOException {
        if(!isRunning) {
            start();
        }
        NioUdpSession session = null;
        try {
            boundLock.lock();
            if (ioProcessBound >= ioProcess.length) {
                ioProcessBound = 0;
            }
            NioUdpIoProcess process = ioProcess[ioProcessBound];

            DatagramChannel dc = DatagramChannel.open();
            if(localAddress != null) {
                dc.bind(localAddress);
            }
            dc.configureBlocking(false);
            dc.connect(remoteAddress);

            session = new NioUdpSession(this, dc, remoteAddress, dc.getLocalAddress(), process);

            NioUdpSession finalSession = session;
            process.addPendingTask(() -> {
                try {
                    dc.register(process.selector, SelectionKey.OP_READ, finalSession);
                } catch (ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            });
            process.wakeup();
            ioProcessBound++;
            return session;
        } catch (Exception e) {
            throw e;
        } finally {
            boundLock.unlock();
            if(session != null) {
                session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CONNECTION, session, this));
            }
        }
    }


    @Override
    public void start() throws IOException {
        super.start();
        ioProcess = new NioUdpIoProcess[ioThreadTotal];
        for (int i = 0; i < ioThreadTotal; i++) {
            ioProcess[i] = new NioUdpIoProcess(this, i);
            ioProcess[i].start();
        }
    }

    @Override
    public void stop() throws IOException {
        if(!isRunning) {
            return;
        }
        isRunning = false;
        for (int i = 0; i < ioThreadTotal; i++) {
            Utils.closeable(ioProcess[i]);
        }
        sessions.clear();
    }

}
