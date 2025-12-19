package or.lotus.core.nio.udp;

import or.lotus.core.nio.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class NioUdpSession extends Session {
    DatagramChannel channel;
    SocketAddress remoteAddress;

    public NioUdpSession(NioContext context, DatagramChannel datagramChannel, SocketAddress remoteAddress, IoProcess ioProcess) {
        super(context, ioProcess);
        this.remoteAddress = remoteAddress;
        channel = datagramChannel;
    }

    @Override
    public void write(Object data) {
        super.write(data);
        ioProcess.addPendingTask(() -> {
            //在io线程运行并发送数据
            Object msg;
            LotusByteBuffer out;
            while((msg = waitSendMessageList.poll()) != null) {
                boolean sent = true;
                do {
                    out = (LotusByteBuffer) context.pulledByteBuffer();
                    try {
                        sent = getCodec().encode(this, msg, out);
                        if(isClosed()) {
                            return;
                        }
                        ByteBuffer[] buffers = out.getAllDataBuffer();
                        if(buffers.length <= 0) {
                            return;
                        }
                        for(ByteBuffer buff : buffers) {
                            buff.flip();
                            while(buff.hasRemaining()) {//保证写完
                                channel.write(buff);
                            }
                        }
                    } catch (Exception e) {
                        pushEventRunnable(new IoEventRunnable(e, IoEventRunnable.IoEventType.SESSION_EXCEPTION, this, context));
                    } finally {
                        out.release();
                    }
                } while (sent == false);
            }
        });
        ioProcess.wakeup();
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

}
