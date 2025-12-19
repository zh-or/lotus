package or.lotus.core.nio.udp;

import or.lotus.core.nio.NioContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;

public class NioUdpServer extends NioContext {

    protected DatagramChannel serverChannel;
    protected NioUdpIoProcess[] ioProcesses;
    protected boolean isEnableBroadcast = false;
    protected int socketBufferSize = 1024 * 1024;
    protected ConcurrentHashMap<SocketAddress, NioUdpSession> udpSessions = new ConcurrentHashMap<>();

    public NioUdpServer(int maxBufferCount, int bufferCapacity, boolean useDirectBuffer) {
        super(maxBufferCount, bufferCapacity, useDirectBuffer);
    }

    public NioUdpServer(int maxBufferCount, int bufferCapacity, int selectorThreadTotal, boolean useDirectBuffer) {
        super(maxBufferCount, bufferCapacity, selectorThreadTotal, useDirectBuffer);
        if(bufferCapacity <= 0) {
            bufferCapacity = 65507;//udp 最大数据包大小
        }
    }

    public void enableBroadcast() {
        isEnableBroadcast = true;
    }

    @Override
    public void bind(InetSocketAddress address) throws IOException {
        if(serverChannel == null) {
            serverChannel = DatagramChannel.open();
            serverChannel.setOption(StandardSocketOptions.SO_BROADCAST, isEnableBroadcast); // 如无需广播

            serverChannel.configureBlocking(false);
            serverChannel.setOption(java.net.StandardSocketOptions.SO_RCVBUF, socketBufferSize);
        }
        serverChannel.bind(address);
    }

    @Override
    public synchronized void start() throws IOException {
        if(serverChannel == null) {
            throw new RuntimeException("请先绑定端口");
        }
        isRunning = true;
        ioProcesses = new NioUdpIoProcess[selectorThreadTotal];
        for (int i = 0; i < selectorThreadTotal; i++) {
            ioProcesses[i] = new NioUdpIoProcess(this);
            ioProcesses[i].start();
        }
    }

    @Override
    public synchronized void stop() throws IOException {
        if(!isRunning) {
            return;
        }
        isRunning = false;
        for (int i = 0; i < selectorThreadTotal; i++) {
            ioProcesses[i].close();
        }
        if(serverChannel != null) {
            serverChannel.close();
        }
    }

    public int getSocketBufferSize() {
        return socketBufferSize;
    }

    public void setSocketBufferSize(int socketBufferSize) {
        this.socketBufferSize = socketBufferSize;
    }
}
