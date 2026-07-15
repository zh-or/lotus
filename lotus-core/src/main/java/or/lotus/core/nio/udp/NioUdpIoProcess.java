package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.*;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class NioUdpIoProcess extends IoProcess {
    protected Selector selector = null;
    protected NioUdpServer server = null;
    protected int bound;

    public NioUdpIoProcess(NioContext context, int bound) throws IOException {
        super(context);
        selector = Selector.open();
        this.bound = bound;
        if(context instanceof NioUdpServer) {
            server = (NioUdpServer) context;
        }
    }

    @Override
    public void wakeup() {
        if(selector != null) {
            selector.wakeup();
        }
    }

    @Override
    public void process() {
        NioUdpSession session = null;
        try {
            if(selector.select(context.getSelectTimeout()) == 0) {
                //空闲的时候处理
                if(context.getSessionIdleTime() != 0) {
                    handleIdle();
                }
                Utils.SLEEP(1);
                return;
            }

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while(keys.hasNext()) {
                if (!context.isRunning()) {
                    //已经停止了
                    return;
                }

                SelectionKey key = keys.next();
                keys.remove();

                if(key.isValid() && key.isReadable()) {
                    ByteBuffer buff = context.getByteBufferFormCache();
                    DatagramChannel dc = (DatagramChannel) key.channel();
                    SocketAddress clientAddress = null;
                    try {
                        clientAddress = dc.receive(buff);
                    } finally {
                        if(clientAddress == null) {
                            context.putByteBufferToCache(buff);
                            continue;
                        }
                    }
                    //当server不为空表示当前使用的NioUdpServer, session根据clientAddress从server.udpSessions中获取
                    if(server != null) {
                        session = server.udpSessions.get(clientAddress);
                        if(session == null) {
                            session = new NioUdpSession(context, dc, clientAddress, dc.getLocalAddress(), this);
                            server.udpSessions.put(clientAddress, session);
                            session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CONNECTION, session, context));
                        }
                    } else {
                        //当server为空表示当前使用的NioUdpClient, session从 SelectionKey的atth中获取
                        session = (NioUdpSession) key.attachment();
                        if(session == null) {
                            continue;
                        }
                    }


                    LotusByteBuffer readCache = session.getReadCache(buff);
                    if(readCache.getDataLength() > 0) {
                        ProtocolDecoderOutput out = new ProtocolDecoderOutput();
                        boolean hasPack;
                        do {
                            session.setLastActive(System.currentTimeMillis());
                            readCache.flip();
                            hasPack = session.getCodec().decode(session, readCache, out);
                            if(session.isClosed()) {
                                return;
                            }
                            if(hasPack) {
                                session.pushEventRunnable(new IoEventRunnable(out.read(), IoEventRunnable.IoEventType.SESSION_RECEIVE_DATA, session, context));
                            }
                            readCache.compact();
                        } while (hasPack && readCache.getDataLength() > 0);
                    }
                }
            }

        } catch (Exception e) {
            log.debug("读写取数据出错:" + session, e);
            if(session != null) {
                session.closeNow();
                session.pushEventRunnable(new IoEventRunnable(e, IoEventRunnable.IoEventType.SESSION_EXCEPTION, session, context));
            }
        }
    }


    protected void handleIdle() {
        long nowTime = System.currentTimeMillis();
        if(server != null) {
            //只有第一个线程才处理空闲事件
            if(bound == 0) {
                server.udpSessions.forEach((key, session) -> {
                    if(session != null && nowTime - session.getLastActive() >= context.getSessionIdleTime()) {
                        /*call on idle */
                        session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_IDLE, session, context));
                    }
                });
            }
        } else {
            Iterator<SelectionKey> keys = selector.keys().iterator();
            while(keys.hasNext()) {
                SelectionKey key = keys.next();
                if(!context.isRunning()) break;

                if (key.isValid() == false) {
                    continue;
                }
                NioUdpSession session = (NioUdpSession) key.attachment();
                if(session != null && !session.isClosed() && nowTime - session.getLastActive() >= context.getSessionIdleTime()) {
                    /*call on idle */
                    session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_IDLE, session, context));
                }
            }
        }
    }


    @Override
    public void close() {

        if(selector != null) {
            selector.wakeup();
        }
        //等待 process 方法结束
        super.close();

        try {
            if(selector != null) {
                selector.close();
            }
        } catch (Exception e) {
            log.debug("关闭NioUdpIoProcess->selector出错:", e);
        }
    }
}
