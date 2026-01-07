package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
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
    public boolean write(Object data) {
        boolean r = super.write(data);
        ioProcess.addPendingTask(() -> {
            //在io线程运行并发送数据
            Object msg;
            EncodeOutByteBuffer out;
            while((msg = waitSendMessageList.poll()) != null) {
                boolean sent = true;
                do {
                    out = new EncodeOutByteBuffer(context);
                    try {
                        sent = getCodec().encode(this, msg, out);
                        if(isClosed()) {
                            return;
                        }
                        EncodeOutByteBuffer.OutWrapper[] buffers = out.getAllDataBuffer();
                        if(buffers.length <= 0) {
                            return;
                        }
                        for(EncodeOutByteBuffer.OutWrapper buff : buffers) {
                            if(buff.isBuffer) {
                                buff.buffer.flip();
                                int send;
                                while(buff.buffer.hasRemaining()) {//保证写完
                                    send = channel.write(buff.buffer);
                                    if(send == 0 && buff.buffer.hasRemaining()) {
                                        //防止cpu 100%
                                        Utils.SLEEP(1);
                                    }
                                }
                            } else {
                                long loss = buff.size;
                                long start = buff.pos;
                                long send;
                                while(loss > 0) {//保证写完
                                    send = buff.fileChannel.transferTo(start, loss, channel);
                                    start += send;
                                    loss -= send;
                                    if(send == 0 && loss > 0) {
                                        //防止cpu 100%
                                        Utils.SLEEP(1);
                                    }
                                }
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
        return r;
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
