package or.lotus.core.nio;

import or.lotus.core.nio.http.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class Session {
    protected ConcurrentHashMap<Object, Object> attrs = null;
    protected NioContext context = null;
    protected long lastActive = 0l;
    protected long createTime = 0l;
    protected volatile boolean closed = false;
    protected volatile boolean isRunningEvent = false;
    protected ProtocolCodec codec = null;
    protected IoHandler handler = null;
    protected LinkedBlockingQueue<Runnable> eventList = null;
    protected LinkedBlockingQueue<Object> waitSendMessageList = null;
    protected int id;
    protected LotusByteBuffer readCache = null;

    protected IoProcess ioProcess = null;
    protected ReadWriteLock codecRwLock = new ReentrantReadWriteLock();
    public Session(NioContext context, IoProcess ioProcess) {
        this.context = context;
        this.ioProcess = ioProcess;
        synchronized (context.sessions) {
            do {
                id = context.nextSessionId();
            } while(context.sessions.get(id) != null);

            context.sessions.put(id, this);
        }

        createTime = System.currentTimeMillis();
        attrs = new ConcurrentHashMap<Object, Object>();
        codec = context.getProtocolCodec();
        handler = context.getHandler();
        eventList = new LinkedBlockingQueue<Runnable>();
        waitSendMessageList = new LinkedBlockingQueue<>(context.maxMessageSendListCapacity);
    }

    /** 如果消息有需要释放的资源, 请实现 AutoCloseable 接口, 当消息编码完成并写出到socket后会调用 AutoCloseable.close() */
    public boolean write(Object data) {
        return waitSendMessageList.add(data);
    }

    public int getWaitSendMessageCount() {
        return waitSendMessageList.size();
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * 立即关闭该链接
     */
    public void closeNow() {
        synchronized (this) {
            if(closed) return ;
            closed = true;
        }
        synchronized (context.sessions) {
            context.sessions.remove(id);
        }
        attrs.clear();
        ioProcess.addPendingTask(() -> {
            if(readCache != null) {
                while(readCache.release() == false);
            }
        });
        pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CLOSE, this, context));
        waitSendMessageList.stream().forEach(obj -> {
            if(obj instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) obj).close();
                } catch (Exception e) {
                    HttpServer.log.error("msg close error:", e);
                }
            }
        });
        waitSendMessageList.clear();
    }

    public void pushEventRunnable(Runnable run) {
        //保证事件并发时的顺序, 用同一个线程执行
        synchronized (eventList) {
            if(!isRunningEvent) {
                isRunningEvent = true;
                context.executeEvent(run);
            }else{
                eventList.add(run);
            }
        }
    }

    protected Runnable pullEventRunnable() {
        Runnable run = eventList.poll();
        if(run == null) {
            synchronized (eventList) {
                run = eventList.poll();
                if(run == null) {
                    isRunningEvent = false;
                }
            }
        }
        return run;
    }

    public void setReadCache(LotusByteBuffer readCache) {
        this.readCache = readCache;
    }

    public synchronized LotusByteBuffer getReadCache() {
        if(readCache == null) {
            readCache = (LotusByteBuffer) context.pulledByteBuffer();
        }
        return readCache;
    }

    public void setLastActive(long timeMillis) {
        lastActive = timeMillis;
    }

    public long getLastActive() {
        return lastActive;
    }

    public long getCreateTime() {
        return createTime;
    }

    public Object getAttr(Object key, Object defVal) {
        Object val = attrs.get(key);
        if(val == null) return defVal;
        return val;
    }

    public Object getAttr(Object key) {
        return getAttr(key, null);
    }

    public void setAttr(Object key, Object val) {
        attrs.put(key, val);
    }

    public Object removeAttr(Object key) {
        return attrs.remove(key);
    }

    public int getId() {
        return id;
    }

    public  ProtocolCodec getCodec() {
        try {
            codecRwLock.readLock().lock();
            return codec;
        } finally {
            codecRwLock.readLock().unlock();
        }
    }

    public void setCodec(ProtocolCodec codec) {
        try {
            codecRwLock.writeLock().lock();
            this.codec = codec;
        } finally {
            codecRwLock.writeLock().unlock();
        }
    }

    public IoHandler getHandler() {
        return handler;
    }

    public void setHandler(IoHandler handler) {
        this.handler = handler;
    }

    public abstract InetSocketAddress getRemoteAddress();

    public abstract InetSocketAddress  getLocalAddress();

}
