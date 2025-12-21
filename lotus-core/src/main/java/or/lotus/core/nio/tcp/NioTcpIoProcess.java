package or.lotus.core.nio.tcp;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class NioTcpIoProcess extends IoProcess {

    protected LinkedBlockingQueue<NioTcpSession> connectedSession = null;
    protected Selector selector = null;

    public NioTcpIoProcess(NioContext context) throws IOException {
        super(context);
        connectedSession = new LinkedBlockingQueue<NioTcpSession>();
        selector = Selector.open();
    }

    public void putChannel(SocketChannel client) throws IOException {

        NioTcpSession session = new NioTcpSession((NioTcpServer) context, client, this);

        if(!connectedSession.add(session)) {
            throw new RuntimeException("待处理连接队列已满:" + connectedSession.size());
        }
        selector.wakeup();
    }

    @Override
    public void wakeup() {
        if(selector != null) {
            selector.wakeup();
        }
    }

    @Override
    public void process() {
        //1. 处理连接的session
        handleConnected();
        //selector.wakeup();

        //2. 处理事件
        try {
            if(selector.select(context.getSelectTimeout()) == 0) {
                //空闲的时候处理? 先这样
                if(context.getSessionIdleTime() != 0) {
                    handleIdle();
                }
                Utils.SLEEP(1);
                return;
            }
        } catch (IOException e) {
            log.error("轮训事件出错:", e);
        }

        NioTcpSession session;
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

        while(keys.hasNext()) {
            if (!context.isRunning()) {
                //已经停止了
                return;
            }

            SelectionKey key = keys.next();
            keys.remove();
            session = (NioTcpSession) key.attachment();
            if(session == null) {
                try {
                    key.channel().close();
                    continue;
                } catch (IOException e) {
                    log.debug("无session的连接?", e);
                }
            }
            if(session.isClosed()) {
                continue;
            }
            if(!key.isValid()) {
                //key已经被关闭
                session.closeNow();
                continue;
            }
            try {
                if(key.isReadable() && !session.isClosed()) {/*call decode */
                    handleReadData(key, session);
                }
                /*这里判断 key.isValid() 是因为在上面读的时候可能出现连接断开了*/
                if(key.isValid() && key.isWritable() && !session.isClosed()) {
                    handleWriteData(key, session);
                }
            } catch (IOException e) {
                log.debug("读写取数据出错:" + session.getId(), e);
                session.closeNow();
            } catch (Exception e) {
                session.pushEventRunnable(new IoEventRunnable(e, IoEventRunnable.IoEventType.SESSION_EXCEPTION, session, context));
            }
        }
    }

    protected void handleConnected() {
        NioTcpSession session;
        while((session = connectedSession.poll()) != null) {
            try {

                IoHandler handler = session.getHandler();
                try {
                    handler.onBeforeConnection(session);
                } catch (Exception e) {
                    log.debug("连接前处理出错:", e);
                    session.channel.close();
                    session = null;
                    return;
                }
                session.channel.configureBlocking(false);
            } catch (IOException e) {
                log.debug("连接 配置异步/关闭 出错:", e);
            }

            try {
                session.key = session.channel.register(selector, SelectionKey.OP_READ, /*atth*/session);
            } catch (ClosedChannelException e) {
                log.debug("链接在连接成功之前关闭:", e);
                continue;
            }
            session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CONNECTION, session, context));
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
            NioTcpSession session = (NioTcpSession) key.attachment();

            if(session != null && !session.isClosed() && nowTime - session.getLastActive() >= context.getSessionIdleTime()) {
                /*call on idle */
                session.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_IDLE, session, context));
            }
        }
    }

    protected void handleReadData(SelectionKey key, NioTcpSession session) throws Exception {
        LotusByteBuffer sessionReadCache = session.getReadCache();
        int readLen;
        boolean hasPack = false;

        do {
            readLen = session.channel.read(sessionReadCache.getCurrentWriteBuffer());

            if(readLen < 0) {/*EOF*/
                session.closeNow();
                return;
            }
            if(readLen > 0) {
                session.setLastActive(System.currentTimeMillis());
                ProtocolDecoderOutput out = new ProtocolDecoderOutput();
                sessionReadCache.flip();
                hasPack = session.getCodec().decode(session, sessionReadCache, out);
                if(session.isClosed()) {
                    return;
                }
                if(hasPack) {
                    session.pushEventRunnable(new IoEventRunnable(out.read(), IoEventRunnable.IoEventType.SESSION_RECEIVE_DATA, session, context));
                }
                sessionReadCache.compact();
            }
        } while(readLen > 0 && hasPack/*没有收到正确的包则不一致接收, 以免恶意数据导致一直申请内存导致爆炸*/);
    }

    protected void handleWriteData(SelectionKey key, NioTcpSession session) throws Exception {
        Object msg;
        LotusByteBuffer out;
        while((msg = session.pollMessage()) != null) {
            boolean sent;
            do {
                out = (LotusByteBuffer) context.pulledByteBuffer();
                try {
                    sent = session.getCodec().encode(session, msg, out);
                    if(session.isClosed()) {
                        return;
                    }
                    ByteBuffer[] buffers = out.getAllDataBuffer();
                    if(buffers.length <= 0) {
                        return;
                    }

                    for(ByteBuffer buff : buffers) {
                        buff.flip();
                        while(buff.hasRemaining()) {//保证写完
                            session.channel.write(buff);
                        }
                    }
                } finally {
                    out.release();
                }
                session.setLastActive(System.currentTimeMillis());
                context.executeEvent(new IoEventRunnable(msg, IoEventRunnable.IoEventType.SESSION_SENT, session, context));
            } while(sent == false);
        }

        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
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
            log.debug("关闭NioTcpIoProcess->selector出错:", e);
        }
    }
}
