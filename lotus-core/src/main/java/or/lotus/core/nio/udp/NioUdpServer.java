package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.NioContext;
import or.lotus.core.nio.tcp.NioTcpIoProcess;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
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

    public NioUdpServer(int maxBufferCount, int bufferCapacity, boolean useDirectBuffer) {
        super(maxBufferCount, bufferCapacity, useDirectBuffer);
    }

    public NioUdpServer(int maxBufferCount, int bufferCapacity, boolean useDirectBuffer, int selectorThreadTotal) {
        super(maxBufferCount, bufferCapacity, useDirectBuffer, selectorThreadTotal);
        if(bufferCapacity <= 0) {
            this.bufferCapacity = 65507;//udp 最大数据包大小
        }
        bindAddress = new ArrayList<>(2);
    }


    /** 设置需要监听的端口, 支持绑定多个端口, 将在start时绑定 */
    @Override
    public void bind(InetSocketAddress address) {
        bindAddress.add(address);
    }

    @Override
    public synchronized void start() throws IOException {
        if(isRunning) {
            throw new RuntimeException("当前服务已启动.");
        }
        isRunning = true;

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
        ioProcess = new NioUdpIoProcess[Math.min(selectorThreadTotal, addressTotal)];

        for(int i = 0; i < addressTotal; i++) {
            int ioBound = ioProcess.length % addressTotal;
            if(ioProcess[ioBound] == null) {
                ioProcess[ioBound] = new NioUdpIoProcess(this);
            }
            serverChannel[i].register(ioProcess[ioBound].selector, SelectionKey.OP_READ);
        }

        for(NioUdpIoProcess ip : ioProcess) {
            ip.start();
        }
    }

    @Override
    public synchronized void stop() throws IOException {
        if(!isRunning) {
            return;
        }
        isRunning = false;

        for(NioUdpIoProcess ip : ioProcess) {
            Utils.closeable(ip);
        }
        for(DatagramChannel dc : serverChannel) {
            Utils.closeable(dc);
        }
    }
}
