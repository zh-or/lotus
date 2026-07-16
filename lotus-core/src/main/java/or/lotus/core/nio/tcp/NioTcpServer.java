package or.lotus.core.nio.tcp;


import or.lotus.core.common.Utils;
import or.lotus.core.nio.NioContext;
import or.lotus.core.nio.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioTcpServer extends NioContext {
    public static final Logger log = LoggerFactory.getLogger(NioTcpServer.class);

    protected ServerSocketChannel serverSocketChannel;
    protected NioTcpIoProcess[] ioProcesses;
    protected NioTcpAcceptor acceptor;
    protected int socketSoTimeout = 3000;//设置 Socket 读取操作的超时时间（SO_TIMEOUT）
    protected boolean tcpNoDelay = false;

    protected int selectorZeroEvent = 512;

    public NioTcpServer() {
        this(1024 * 3, 4 * 1024, false);
    }

    public NioTcpServer(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer) {
        super(cacheBufferSize, bufferCapacity, useDirectBuffer);
    }

    /***
     * 创建一个TCP服务端
     * @param cacheBufferSize ByteBuffer缓存数量
     * @param bufferCapacity 单个ByteBuffer 最小大小
     * @param ioThreadTotal io线程数量
     * @param useDirectBuffer 是否使用直接内存
     */
    public NioTcpServer(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer, int ioThreadTotal) {
        super(cacheBufferSize, bufferCapacity, useDirectBuffer, ioThreadTotal);
    }

    public int getSocketSoTimeout() {
        return socketSoTimeout;
    }

    public NioTcpServer setSocketSoTimeout(int socketSoTimeout) {
        this.socketSoTimeout = socketSoTimeout;
        return this;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public NioTcpServer setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    /**
     * 检测epoll空循环次数, 默认为512
     * */
    public void setSelectorZeroEventCount(int selectorZeroEvent) {
        this.selectorZeroEvent = selectorZeroEvent;
    }

    @Override
    public synchronized void bind(InetSocketAddress address) throws IOException {
        if(serverSocketChannel == null) {
            serverSocketChannel = ServerSocketChannel.open();
        }
        serverSocketChannel.bind(address);
    }

    @Override
    public synchronized void start() throws IOException {
        if(serverSocketChannel == null) {
            throw new RuntimeException("请先绑定端口");
        }
        super.start();
        serverSocketChannel.configureBlocking(false);

        ioProcesses = new NioTcpIoProcess[ioThreadTotal];
        for (int i = 0; i < ioThreadTotal; i++) {
            ioProcesses[i] = new NioTcpIoProcess(this);
            ioProcesses[i].start();
        }
        acceptor = new NioTcpAcceptor();
        acceptor.start();
    }


    @Override
    public synchronized void stop()  {

        if (!isRunning) {
            return;
        }
        super.stop();
        acceptor.close();
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
        if(serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.debug("关闭serverSocketChannel出错:", e);
            }
        }
    }

    class NioTcpAcceptor extends Thread {
        Selector selector;

        NioTcpAcceptor() throws IOException {
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            setName("lotus accept thread");
        }

        public void close() {
            if(selector != null) {
                selector.wakeup();
            }
            try {
                join(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if(selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    log.debug("关闭selector出错:", e);
                }
            }
        }

        @Override
        public void run() {
            int ioProcessBound = 0;
            while (isRunning()) {
                try {
                    if(selector.select(selectTimeout) == 0) {
                        Utils.SLEEP(1);
                        continue;
                    }

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while(keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();
                        if(key.isAcceptable()) {
                            try {
                                SocketChannel client = serverSocketChannel.accept();
                                if(client == null) continue;
                                if(socketSoTimeout > 0) {
                                    client.socket().setSoTimeout(socketSoTimeout);
                                }
                                if(tcpNoDelay) {
                                    client.socket().setTcpNoDelay(true);
                                }
                                //不用设置此值, 看起来操作系统会自动优化
                                //client.socket().setReceiveBufferSize(buff_cache_size);
                                //client.socket().setSendBufferSize(buff_cache_size);

                                try {
                                    if(ioProcessBound >= ioProcesses.length) {
                                        ioProcessBound = 0;
                                    }
                                    ioProcesses[ioProcessBound].putChannel(client, false);
                                    ioProcessBound ++;
                                } catch (Exception e) {
                                    log.debug("处理连接失败:", e);
                                }
                            } catch (Exception e) {
                                log.debug("接收连接失败:", e);
                            }
                        }
                    }

                } catch (IOException e) {
                    log.error("acceptor selector error:", e);
                    continue;
                }
            }
        }
    }

}
