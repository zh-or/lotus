package lotus.nio.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import lotus.nio.IoEventRunnable;
import lotus.nio.IoEventRunnable.IoEventType;
import lotus.nio.IoProcess;
import lotus.nio.LotusIOBuffer;
import lotus.nio.NioContext;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;
import lotus.utils.Utils;


public class NioTcpIoProcess extends IoProcess implements Runnable {
	private Selector                    selector    = null;

    public NioTcpIoProcess(NioContext context) throws IOException {
    	super(context);
		selector = Selector.open();
		tmp_buffer = new byte[context.getSessionCacheBufferSize()];
	}

    public NioTcpSession putChannel(SocketChannel channel, long id) throws Exception {
        return putChannel(channel, id, true);
    }

    /**
     *
     * @param channel
     * @param id
     * @param connectionFinished 是否已完成连接
     * @return
     * @throws Exception
     */
    public NioTcpSession putChannel(final SocketChannel channel, long id, final boolean connectionFinished) throws Exception {
        if(channel == null || selector == null) {
            throw new Exception("null");
        }

        final NioTcpSession session = new NioTcpSession(context, channel, this, id);
        context.getEventExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SelectionKey key = null;

                    if(connectionFinished) {
                        callOnBeforeConnection(channel, session);
                        /*call on connection*/
                        /*register 时会等待 selector.select() 所以此处先唤醒selector以免锁冲突 */
                        selector.wakeup();
                        key = channel.register(selector, SelectionKey.OP_READ, session);

                        session.setKey(key);
                        selector.wakeup();
                        session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CONNECTION, session, context));
                    } else {
                        //这里有需要处理 onBefore 事件

                        /*register 时会等待 selector.select() 所以此处先唤醒selector以免锁冲突 */
                        selector.wakeup();
                        key = channel.register(selector, SelectionKey.OP_CONNECT, session);
                        session.setKey(key);
                        selector.wakeup();
                    }


                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return session;
    }

    private void callOnBeforeConnection(SocketChannel channel, Session session) throws Exception {
        if(!session.getEventHandler().onBeforeConnection(session)) {
            channel.close();
            return;
        }

        channel.configureBlocking(false);

        if(session.hasReadCache()) {
            ByteBuffer readcache = session.getReadCacheBuffer();
            handleReadData(readcache, session);
        }
    }

    @Override
    public void run() {
        while(isrun && selector != null) {
            try {
                handleIoEvent();

                if(context.getSessionIdleTimeOut() != 0) {
                    try {
                        handleTimeOut();
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	/*缓存buff的状态*/
	private volatile int remaining = 0;
	private volatile int capacity  = 0;
	private volatile int limit     = 0;

	private byte[] tmp_buffer;

    private void handleIoEvent() throws IOException {
        if(selector.select(NioContext.SELECT_TIMEOUT) == 0) {
            if(!isrun) return;
            Utils.SLEEP(1);
            return;
        }
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

        while(keys.hasNext()){
            if(!isrun) break;

            SelectionKey key = keys.next();
            keys.remove();

            NioTcpSession session = (NioTcpSession) key.attachment();
            if(session == null) {
                continue;
            }

            try {
                if(!key.isValid()) {
                    session.closeNow();
                    continue;
                }
                if(key.isReadable()) {/*call decode */
//                    long s = System.currentTimeMillis();
                    ByteBuffer readcache = session.getReadCacheBuffer();
                    int len = session.getChannel().read(readcache);
                    if(len < 0){/*EOF*/
                        session.closeNow();
                        context.putByteBufferToCache(readcache);
                        session.updateReadCacheBuffer(null);
                        continue;
                    }else {
                        session.setLastActive(System.currentTimeMillis());
                        //capacity 容量
                        //limit 也就是缓冲区可以利用（进行读写）的范围的最大值
                        //position 当前读写位置
                        readcache.flip();
                        while(handleReadData(readcache, session));
                        //selector.wakeup();
                    }
                }

                if(key.isWritable()) {/*call encode*/
                    handleWriteData(session);
                }

                if(key.isConnectable()) {
                    handleConnection(key, session);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                /*call exception*/
                //session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                //e.printStackTrace();
                session.closeNow();
                //cancelKey(key);/*对方关闭了?*/
            }
        }
    }

    private void handleTimeOut() {
        Iterator<SelectionKey> keys = selector.keys().iterator();
        final long nowtime = System.currentTimeMillis();
        while(keys.hasNext()){
            SelectionKey key = keys.next();/*这里报错?*/
            if(!isrun) break;

            if (key.channel() instanceof ServerSocketChannel) {
                continue;
            }

            if (key.isValid() == false) {
                continue;
            }
            NioTcpSession session = (NioTcpSession) key.attachment();
            if(session != null && nowtime - session.getLastActive() > context.getSessionIdleTimeOut()) {
                /*call on idle */
                session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_IDLE, session, context));

            }
        }
    }

    private boolean handleReadData(ByteBuffer readcache, Session session) {
        //System.out.println("readCache:" + session.getId() + " data:" + readcache);
        ProtocolDecoderOutput msgout = session.getProtocolDecoderOutput();
        boolean ishavepack = false;

        try {
            ishavepack = session.getProtocoCodec().decode(session, readcache, msgout);
        } catch (Exception e) {
            session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
        }

        remaining = readcache.remaining();
        limit     = readcache.limit();
        if(remaining <= 0){//用完了回收掉
            context.putByteBufferToCache(readcache);
            session.updateReadCacheBuffer(null);
        }else{
            //copyData(readcache);
            if(remaining >= readcache.capacity()){/*已经读满了, 缓存不够. 就不写环形缓冲队列了 :(*/
                readcache.rewind();//重置 position 位置为 0 并忽略 mark
                capacity = readcache.capacity();
                if(capacity > tmp_buffer.length){//扩过容且缓存不够
                    tmp_buffer = null;
                    tmp_buffer = new byte[capacity];
                }
                readcache.get(tmp_buffer, 0, limit);
                int newLen = readcache.capacity() * 2;;
                context.putByteBufferToCache(readcache);/*回收了*/
                ByteBuffer newreadcache = context.getByteBufferFormCache(newLen);
                /*扩容后这便是一个新的obj了, 故手动更新*/
                newreadcache.put(tmp_buffer, 0, limit);
                session.updateReadCacheBuffer(newreadcache);/*update*/
            }else{
                readcache.compact();//把未读的数据复制到缓冲区起始位置 此时 position 为数据结尾
                session.updateReadCacheBuffer(readcache);/*update*/
            }
        }

        if(ishavepack) {
            session.pushEventRunnable(new IoEventRunnable(msgout.read(), IoEventType.SESSION_RECVMSG, session, context));
            msgout.write(null);
            return readcache.hasRemaining();
        }
        return false;
    }

    private void handleWriteData(NioTcpSession session) {
        Object msg = session.poolMessage();
        if(msg != null) {

            boolean repSend = false;
            do {
                LotusIOBuffer out = new LotusIOBuffer(context);
                try {
                    //编码解码器放session更好
                    repSend = !session.getProtocoCodec().encode(session, msg, out);
                    ByteBuffer[] buffers = out.getAllBuffer();
                    for(ByteBuffer buff : buffers) {
                        buff.flip();
                        while(buff.hasRemaining()) {
                            session.write(buff);
                        }
                    }
                    out.free();
                    out = null;
                } catch (Exception e) {
                    session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                }
                session.setLastActive(System.currentTimeMillis());
            } while(repSend);

            /*call message sent*/
            session.pushEventRunnable(new IoEventRunnable(msg, IoEventType.SESSION_SENT, session, context));
        }else{
            Object msglock = session.getMessageLock();
            synchronized (msglock) {
                if(session.getWriteMessageSize() <= 0){
                    session.removeInterestOps(SelectionKey.OP_WRITE);
                }
            }

            if(session.isSentClose()){
                session.closeNow();
            }
        }
    }

    private void handleConnection(SelectionKey key, NioTcpSession session) {
        synchronized (session) {
            session.notifyAll();
        }
        //Finishes the process of connecting a socket channel.
        //fuck the doc
        try {
            if(((SocketChannel) key.channel()).finishConnect() == false){
                /*连接失败???*/
                cancelKey(key);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        session.removeInterestOps(SelectionKey.OP_CONNECT);//取消连接成功事件
        session.addInterestOps(SelectionKey.OP_READ);//注册读事件
        session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CONNECTION, session, context));
    }

    public void cancelKey(SelectionKey key) {
        if(key == null) return;
        try {
            Session session = (Session) key.attachment();
            session.closeNow();
            key.channel().close();
            key.cancel();
        } catch (Exception e) {}
    }

    public void close() {
        isrun = false;
        selector.wakeup();
        try {
            Iterator<SelectionKey> it = selector.keys().iterator();
            while(it.hasNext()){
                cancelKey(it.next());
            }
            selector.selectNow();
            selector.close();
            selector.wakeup();
            if(selector != null) selector.close();
        } catch (IOException e) {}
    }

}
