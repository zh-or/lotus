package or.lotus.core.nio.udp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.*;
import or.lotus.core.nio.tcp.NioTcpSession;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

public class NioUdpIoProcess extends IoProcess {
    protected Selector selector = null;
    protected NioUdpServer server;
    public NioUdpIoProcess(NioContext context) throws IOException {
        super(context);
        selector = Selector.open();
        server = (NioUdpServer) context;
        server.serverChannel.register(selector, SelectionKey.OP_READ);
    }

    @Override
    public void wakeup() {
        if(selector != null) {
            selector.wakeup();
        }
    }

    @Override
    public void process() {
        //2. 处理事件
        NioUdpSession session = null;
        try {
            if(selector.select(context.getSelectTimeout()) == 0) {
                //空闲的时候处理? 先这样
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
                    SocketAddress clientAddress = dc.receive(buff);

                    session = server.udpSessions.get(clientAddress);
                    if(session == null) {
                        session = new NioUdpSession(context, dc, clientAddress, this);
                        server.udpSessions.put(clientAddress, session);
                        session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CONNECTION, session, context));
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

        } catch (IOException e) {
            log.debug("读写取数据出错:" + session.getId(), e);
            session.closeNow();
        } catch (Exception e) {
            session.pushEventRunnable(new IoEventRunnable(e, IoEventRunnable.IoEventType.SESSION_EXCEPTION, session, context));
        }
    }



    protected void handleIdle() {
        //只处理当前IoProcess注册的session
        Iterator<SelectionKey> keys = selector.keys().iterator();
        long nowTime = System.currentTimeMillis();
        while(keys.hasNext()) {
            SelectionKey key = keys.next();/*这里报错?*/
            if(!context.isRunning()) break;

            if (key.channel() instanceof ServerSocketChannel) {
                continue;
            }

            if (key.isValid() == false) {
                continue;
            }
            NioUdpSession session = (NioUdpSession) key.attachment();

            if(session != null && nowTime - session.getLastActive() >= context.getSessionIdleTime()) {
                /*call on idle */
                session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_IDLE, session, context));
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
