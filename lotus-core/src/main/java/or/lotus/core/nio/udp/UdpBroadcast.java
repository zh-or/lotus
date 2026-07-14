package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * 有限广播地址: 255.255.255.255 数据包会被限制在当前的物理网段（局域网）内
 * 定向广播地址: 如果当前网络 IP 是 192.168.1.100，子网掩码是 255.255.255.0（即 /24），那么该网段的定向广播地址就是 192.168.1.255
 * 组播地址范围：224.0.0.0 到 239.255.255.255, 只有加入了该特定组播组的设备才能收到数据
 *
 * */
public class UdpBroadcast implements AutoCloseable {

    public static int broadcast(ByteBuffer buff, String host, int port) throws IOException {
        int n = 0;
        UdpBroadcast ub = null;
        try {
            ub = new UdpBroadcast(true);
            n = ub.send(buff, host, port);
        } finally {
            Utils.closeable(ub);
        }
        return n;
    }

    public static int multicast(ByteBuffer buff, String host, int port) throws IOException {
        int n = 0;
        UdpBroadcast ub = null;
        try {
            ub = new UdpBroadcast(false);
            n = ub.send(buff, host, port);
        } finally {
            Utils.closeable(ub);
        }
        return n;
    }


    DatagramChannel datagramChannel;

    /** @param isBroadcast  当需要发送组播时, 此值应传false */
    public UdpBroadcast(boolean isBroadcast) throws IOException {
        datagramChannel = DatagramChannel.open();
        //datagramChannel.configureBlocking(false);
        if(isBroadcast) {
            datagramChannel.socket().setBroadcast(true);
        }
    }

    /** 给当前网段发送 */
    public int send(ByteBuffer buff, int port) throws Exception {
        return send(buff, Utils.getCurrentBroadcastAddress(), port);
    }

    public int send(ByteBuffer buff, String host, int port) throws IOException {
        return datagramChannel.send(buff, new InetSocketAddress(InetAddress.getByName(host), port));
    }

    public DatagramChannel getDatagramChannel() {
        return datagramChannel;
    }

    @Override
    public void close() throws Exception {
        Utils.closeable(datagramChannel);
    }
}
