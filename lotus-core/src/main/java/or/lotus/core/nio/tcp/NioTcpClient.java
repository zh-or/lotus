package or.lotus.core.nio.tcp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.IoEventRunnable;
import or.lotus.core.nio.NioContext;
import or.lotus.core.nio.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

public class NioTcpClient extends NioContext {
    public static final Logger log = LoggerFactory.getLogger(NioTcpClient.class);

    private int ioProcessBound = 0;
    private final ReentrantLock boundLock = new ReentrantLock();
    private NioTcpIoProcess[] ioProcesses;

    public NioTcpClient() {
        this(1024 * 3, 4 * 1024, false);
    }

    public NioTcpClient(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer) {
        super(cacheBufferSize, bufferCapacity, useDirectBuffer);
    }

    public NioTcpClient(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer, int ioThreadTotal) {
        super(cacheBufferSize, bufferCapacity, useDirectBuffer, ioThreadTotal);
    }

    @Override
    public void bind(InetSocketAddress address) throws IOException {
        throw new UnsupportedOperationException("NioTcpClient does not support bind");
    }

    @Override
    public synchronized void start() throws IOException {
        super.start();
        ioProcesses = new NioTcpIoProcess[ioThreadTotal];
        for (int i = 0; i < ioThreadTotal; i++) {
            ioProcesses[i] = new NioTcpIoProcess(this);
            ioProcesses[i].start();
        }
    }

    @Override
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }
        super.stop();
        synchronized (sessions) {
            int size = sessions.size();
            for(int i = 0; i < size; i++) {
                int key = sessions.keyAt(i);
                Session session = sessions.get(key);
                if(session != null) {
                    session.closeNow();
                }
            }
            sessions.clear();
        }

        for (int i = 0; i < ioThreadTotal; i++) {
            ioProcesses[i].close();
        }
    }

    /** 异步连接 */
    public NioTcpSession connection(InetSocketAddress address) throws IOException {
        return connection(address, -1);
    }

    /** 如果 timeout > -1 则为同步连接 */
    public NioTcpSession connection(InetSocketAddress address, int timeout) throws IOException {
        if(!isRunning) {
            try {
                start();
            } catch (IOException e) {
                return null;
            }
        }

        SocketChannel sc = null;
        NioTcpSession session = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(false);

            boolean isConnect = sc.connect(address);

            boundLock.lock();
            try {
                if (ioProcessBound >= ioProcesses.length) {
                    ioProcessBound = 0;
                }
                //如果连接成功则直接进入处理连接事件, 而不是注册 OP_CONNECT
                session = ioProcesses[ioProcessBound].putChannel(sc, !isConnect);
                ioProcessBound++;
            } finally {
                boundLock.unlock();
            }
            if(isConnect) {
                session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CONNECTION, session, this));
                return session;
            }
            if (timeout > -1 && session != null) {
                try {
                    synchronized (session) {
                        session.wait(timeout);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return session;
        } catch (IOException e) {
            Utils.closeable(sc);
            throw e;
        }
    }
}
