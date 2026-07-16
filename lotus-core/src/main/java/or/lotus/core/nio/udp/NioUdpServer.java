package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.NioContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** 注意: 由于java8不支持多channel绑定同一端口所以导致只能单线程收发数据,
 * 如果性能不足可尝试绑定多个端口以解决只占用单个cpu的问题
 * */
public class NioUdpServer extends NioContext {
    protected List<InetSocketAddress> bindAddress;
    protected DatagramChannel[] serverChannel;
    protected NioUdpIoProcess[] ioProcess;
    protected ConcurrentHashMap<SocketAddress, NioUdpSession> udpSessions = new ConcurrentHashMap<>();

    public NioUdpServer() {
        this(1024 * 3, 4 * 1024, false);
    }

    public NioUdpServer(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer) {
        this(cacheBufferSize, bufferCapacity, useDirectBuffer, Runtime.getRuntime().availableProcessors() + 1);
    }

    public NioUdpServer(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer, int ioThreadTotal) {
        super(cacheBufferSize, bufferCapacity, useDirectBuffer, ioThreadTotal);
        if(bufferCapacity <= 0) {
            this.bufferCapacity = 65507;//udp 最大数据包大小
        }
        bindAddress = new ArrayList<>(2);
    }

    /** 根据远程地址端口获取session */
    public NioUdpSession getSessionByAddress(SocketAddress address) {
        return udpSessions.get(address);
    }


    /** 设置需要监听的端口, 支持绑定多个端口, 将在start时绑定 */
    @Override
    public void bind(InetSocketAddress address) {
        bindAddress.add(address);
    }

    @Override
    public synchronized void start() throws IOException {
        super.start();
        int addressTotal = bindAddress.size();

        if(addressTotal == 0) {
            throw new RuntimeException("当前未设置绑定的端口");
        }
        serverChannel = new DatagramChannel[addressTotal];

        for(int i = 0; i < addressTotal; i++) {
            serverChannel[i] = DatagramChannel.open();
            serverChannel[i].configureBlocking(false);
            //设置为和buffer一样大
            serverChannel[i].setOption(java.net.StandardSocketOptions.SO_RCVBUF, bufferCapacity);
            serverChannel[i].bind(bindAddress.get(i));
        }
        ioProcess = new NioUdpIoProcess[Math.min(ioThreadTotal, addressTotal)];

        for(int i = 0; i < addressTotal; i++) {
            int ioBound = i % ioProcess.length;
            if(ioProcess[ioBound] == null) {
                ioProcess[ioBound] = new NioUdpIoProcess(this, i);
            }
            serverChannel[i].register(ioProcess[ioBound].selector, SelectionKey.OP_READ);
        }

        for(NioUdpIoProcess ip : ioProcess) {
            ip.start();
        }
    }

    @Override
    public synchronized void stop() {

        if (!isRunning) {
            return;
        }
        super.stop();
        for(NioUdpIoProcess ip : ioProcess) {
            Utils.closeable(ip);
        }
        for(DatagramChannel dc : serverChannel) {
            Utils.closeable(dc);
        }
        synchronized (sessions) {
            sessions.clear();
        }
        udpSessions.clear();
    }
}
