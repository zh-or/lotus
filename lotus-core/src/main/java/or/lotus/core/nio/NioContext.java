package or.lotus.core.nio;

import or.lotus.core.intmap.SparseArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public abstract class NioContext {
    public static final Logger log = LoggerFactory.getLogger(NioContext.class);
    protected int selectTimeout = 1000;
    protected IoHandler handler = null;
    protected ProtocolCodec protocolCodec = null;
    protected Executor executor = null;
    /** session无操作空闲间隔 毫秒 */
    protected int sessionIdleTime = 0;

    protected int pooledBufferStepCount = 10;
    protected LinkedBlockingQueue<ByteBuffer> bufferList = null;
    protected LinkedBlockingQueue<ByteBuffer> directBufferList = null;

    protected int selectorThreadTotal = 0;

    /** 最小byteBuffer申请大小, 单个buffer大小不能超过int*/
    protected int bufferCapacity  = 2048;

    /** 最大byteBuffer缓存数量
     *  maxBufferCount * bufferSize = 最大缓存内存大小
     * */
    protected int cacheBufferSize = 2048;

    /** session 待发送消息队列最大大小, 消息队列写满后抛出异常 */
    protected int maxMessageSendListCapacity = 1024;
    protected boolean isUseDirectBuffer = false;
    protected volatile boolean isRunning = false;
    protected AtomicInteger sessionId = new AtomicInteger(0);
    protected SparseArray<Session> sessions = new SparseArray<Session>();
    protected ConcurrentHashMap<LotusByteBuffer, List<String>> retainMap = new ConcurrentHashMap<>();

    public NioContext(int cacheBufferSize, int bufferCapacity, boolean useDirectBuffer) {
        this(cacheBufferSize, bufferCapacity, Runtime.getRuntime().availableProcessors() + 1, useDirectBuffer);
    }

    public NioContext(int cacheBufferSize, int bufferCapacity, int selectorThreadTotal, boolean useDirectBuffer) {
        this.cacheBufferSize = cacheBufferSize;
        this.bufferCapacity = bufferCapacity;

        this.selectorThreadTotal = selectorThreadTotal;
        isUseDirectBuffer = useDirectBuffer;
        bufferList = new LinkedBlockingQueue<ByteBuffer>(cacheBufferSize);
        directBufferList = new LinkedBlockingQueue<ByteBuffer>(cacheBufferSize);
    }

    public int nextSessionId() {
        int t = sessionId.incrementAndGet();
        if(t > 0x7ffffff0) {
            sessionId.set(0);
            t = 0;
        }
        return t;
    }

    /** 获取当前对内ByteBuffer大小 */
    public int getByteBufferCapacity() {
        int r = 0;
        for (ByteBuffer item : bufferList) {
            r += item.capacity();
        }
        return r;
    }

    /** 获取当前堆外ByteBuffer大小 */
    public int getDirectByteBufferCapacity() {
        int r = 0;
        for (ByteBuffer item : directBufferList) {
            r += item.capacity();
        }
        return r;
    }

    public long getFlyByByteBufferCount() {
        return flyByteBuffer.sum();
    }

    public int getCachedByteBufferSize() {
        return bufferList.size();
    }

    public int getCachedDirectByteBufferSize() {
        return directBufferList.size();
    }

    public ByteBuffer getByteBufferFormCache() {
        return getByteBufferFormCache(bufferCapacity, isUseDirectBuffer);
    }
    protected LongAdder flyByteBuffer = new LongAdder();

    public ByteBuffer getByteBufferFormCache(int size, boolean useDirectBuffer) {
        ByteBuffer buffer = null;
        if(size <= bufferCapacity) {
            size = bufferCapacity;

            if(useDirectBuffer) {
                buffer = directBufferList.poll();
            }else {
                buffer = bufferList.poll();
            }
        }

        if(buffer == null) {
            if(useDirectBuffer) {
                buffer = ByteBuffer.allocateDirect(size);
            }else {
                buffer = ByteBuffer.allocate(size);
            }
        }
        flyByteBuffer.add(size);
        return buffer;
    }

    public void putByteBufferToCache(ByteBuffer buffer) {
        if(buffer != null) {
            if(buffer instanceof MappedByteBuffer) {
                ByteBufferClear.cleanDirectBuffer(buffer);
            } else {
                flyByteBuffer.add(-buffer.capacity());
                if((buffer.capacity() == bufferCapacity) && (bufferList.size() < cacheBufferSize)) {
                    buffer.clear();
                    if(buffer.isDirect()) {
                        directBufferList.add(buffer);
                    } else {
                        bufferList.add(buffer);
                    }
                }
            }
        }
    }


    public LotusByteBuf pulledByteBuffer() {
        return pulledByteBuffer(isUseDirectBuffer);
    }

    public LotusByteBuf pulledByteBuffer(ByteBuffer buff) {
        LotusByteBuffer buf = new LotusByteBuffer(this, isUseDirectBuffer);
        buf.append(buff);
        return buf;
    }

    public LotusByteBuf pulledByteBuffer(boolean isUseDirectBuffer) {
        return new LotusByteBuffer(this, isUseDirectBuffer);
    }

    public void executeEvent(Runnable run) {
        if(this.executor == null) {
            run.run();
        }else {
            this.executor.execute(run);
        }
    }

    public void setBufferCapacity(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    public void setUseDirectBuffer(boolean useDirectBuffer) {
        isUseDirectBuffer = useDirectBuffer;
    }

    public int getBufferCapacity() {
        return bufferCapacity;
    }

    public int getCacheBufferSize() {
        return cacheBufferSize;
    }


    public IoHandler getHandler() {
        return handler;
    }

    public void setHandler(IoHandler handler) {
        this.handler = handler;
    }

    public ProtocolCodec getProtocolCodec() {
        return protocolCodec;
    }

    public void setProtocolCodec(ProtocolCodec protocolCodec) {
        this.protocolCodec = protocolCodec;
    }

    public Executor getExecutor() {
        return executor;
    }

    /** 解码后handler事件执行的线程池, 可设置为null, 当此线程池为null时事件将直接在io线程运行 */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public int getSessionIdleTime() {
        return sessionIdleTime;
    }

    public void setSessionIdleTime(int sessionIdleTime) {
        this.sessionIdleTime = sessionIdleTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void bind(int port) throws IOException {
        bind(new InetSocketAddress(port));
    }

    /** 绑定网卡以及端口 */
    public abstract void bind(InetSocketAddress address) throws IOException;

    /** 启动服务器 */
    public abstract void start() throws IOException;

    public abstract void stop() throws IOException;


    public int getMaxMessageSendListCapacity() {
        return maxMessageSendListCapacity;
    }

    public void setMaxMessageSendListCapacity(int maxMessageSendListCapacity) {
        this.maxMessageSendListCapacity = maxMessageSendListCapacity;
    }

    /**
     * 获取当前未释放的命名 LotusByteBuf
     */
    public ConcurrentHashMap<LotusByteBuffer, List<String>> getRetainMap() {
        return retainMap;
    }


    public int getSelectTimeout() {
        return selectTimeout;
    }

    public void setSelectTimeout(int selectTimeout) {
        this.selectTimeout = selectTimeout;
    }
}
