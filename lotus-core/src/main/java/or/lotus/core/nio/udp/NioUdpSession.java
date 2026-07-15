package or.lotus.core.nio.udp;

import or.lotus.core.nio.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;

public class NioUdpSession extends Session {
    DatagramChannel channel;
    SocketAddress remoteAddress;
    SocketAddress localAddress;

    public NioUdpSession(NioContext context, DatagramChannel datagramChannel, SocketAddress remoteAddress, SocketAddress localAddress, IoProcess ioProcess) {
        super(context, ioProcess);
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        channel = datagramChannel;
    }

    @Override
    public boolean write(Object data) {
        boolean r = super.write(data);
        Session that = this;
        ioProcess.addPendingTask(() -> {
            //在io线程运行并发送数据
            Object msg;
            EncodeOutByteBuffer out;
            while((msg = waitSendMessageList.poll()) != null) {
                boolean sent = true;
                do {
                    out = new EncodeOutByteBuffer(context, false);
                    try {
                        sent = getCodec().encode(that, msg, out);
                        if(isClosed()) {
                            return;
                        }
                        EncodeOutByteBuffer.OutWrapper[] buffers = out.getAllDataBuffer();
                        if(buffers.length <= 0) {
                            return;
                        }
                        for(EncodeOutByteBuffer.OutWrapper buff : buffers) {
                            buff.buffer.flip();
                            int send = channel.send(buff.buffer, remoteAddress);
                            if(send <= 0) {
                                System.out.println("发送为0");
                            }
                        }
                    } catch (Exception e) {
                        pushEventRunnable(new IoEventRunnable(e, IoEventRunnable.IoEventType.SESSION_EXCEPTION, that, context));
                    } finally {
                        out.release();
                    }
                } while (sent == false);
                that.setLastActive(System.currentTimeMillis());
                context.executeEvent(new IoEventRunnable(msg, IoEventRunnable.IoEventType.SESSION_SENT, that, context));
            }
        });
        ioProcess.wakeup();
        return r;
    }

    /** 作为客户端时加入组播 */
    public MembershipKey joinMulticast(InetAddress group, NetworkInterface interf) throws IOException {
        return channel.join(group, interf);
    }

    /** 作为客户端时加入组播 */
    public MembershipKey joinMulticast(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        return channel.join(group, interf, source);
    }

    public synchronized LotusByteBuffer getReadCache(ByteBuffer buff) {
        if(readCache == null) {
            readCache = (LotusByteBuffer) context.pulledByteBuffer(buff);
        } else {
            readCache.append(buff);
        }
        return readCache;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) remoteAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        try {
            return (InetSocketAddress) channel.getLocalAddress();
        } catch (IOException e) {}
        return null;
    }

    @Override
    public void closeNow() {
        super.closeNow();

    }
}
