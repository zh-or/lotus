package or.lotus.nio;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class Session {
    protected ConcurrentHashMap<Object, Object> attrs           =   null;
    protected NioContext                        context     	=   null;
    protected long                              lastActive      =	0l;
    protected volatile ByteBuffer               readcache       =   null;
    protected LinkedBlockingQueue<Runnable>     eventList       =   null;
    protected volatile boolean                  runingEvent     =   false;
    private   Object                            runingEventObj  =   new Object();
    protected ProtocolDecoderOutput             deout           =   null;
    protected long                              id              =   0l;
    protected final Object                      recvPackwait    =   new Object();
    protected long                              createtime      =   0l;
    protected volatile boolean                  closed          = false;
    protected boolean                           isWaitForRecvPack = false;
    protected Object                            notifyRecvMsg   = null;
    protected ProtocolCodec                     codec           = null;
    protected IoHandler                         handler         = null;

    public Session (NioContext context, long id){
        this.context = context;
        this.attrs = new ConcurrentHashMap<Object, Object>();
        this.id = id;
        this.createtime = System.currentTimeMillis();
        this.codec = context.getProtocoCodec();
        this.handler = context.getEventHandler();
        setLastActive(System.currentTimeMillis());
        eventList = new LinkedBlockingQueue<Runnable>();
        deout = new ProtocolDecoderOutput();

    }

    public IoHandler getEventHandler(){
        return handler;
    }

    public void setIoHandler(IoHandler handler){
        this.handler = handler;
    }

    public void setProtocolCodec(ProtocolCodec codec){
        this.codec = codec;
    }

    public ProtocolCodec getProtocoCodec(){
        return codec;
    }

    public long getId(){
        return id;
    }

    public long getCreateTime(){
        return createtime;
    }

    public abstract int getWriteMessageSize();

    public Object getAttr(Object key, Object defval){
    	Object val = attrs.get(key);
    	if(val == null) return defval;
    	return val;
    }

    public Object getAttr(Object key){
        return getAttr(key, null);
    }

    public void setAttr(Object key, Object val){
    	attrs.put(key, val);
    }

    public Object removeAttr(Object key){
        return attrs.remove(key);
    }

    public synchronized void setLastActive(long t){
        lastActive = t;
    }

    public synchronized long getLastActive(){
    	return lastActive;
    }

    public ProtocolDecoderOutput getProtocolDecoderOutput(){
        return deout;
    }

    public boolean hasReadCache() {
        return readcache != null;
    }

    /**
     * 这个buffer用来缓存已经从io中读取到的数据
     * @return
     */
    public ByteBuffer getReadCacheBuffer() {
        if(readcache == null) {
            return context.getByteBufferFormCache();
        }
    	return readcache;
    }

    public void updateReadCacheBuffer(ByteBuffer buffer) {
        this.readcache = buffer;
    }

    /**
     * 从缓存中获取buffer
     * @param len 大于预设大小则会分配新buffer
     * @return
     */
    public ByteBuffer getWriteCacheBuffer(int len) {
        return context.getByteBufferFormCache(len);
    }

    public void putWriteCacheBuffer(ByteBuffer buff){
        context.putByteBufferToCache(buff);
    }

    public void pushEventRunnable(Runnable run) {
        //保证事件并发时的顺序, 用同一个线程执行
        synchronized (runingEventObj) {
            if(!runingEvent){
                runingEvent = true;
                context.ExecuteEvent(run);
            }else{
                eventList.add(run);
            }
        }
    }

    public NioContext getContext() {
        return context;
    }

    public Runnable pullEventRunnable(){
        Runnable run = eventList.poll();
        if(run == null) {
            synchronized (runingEventObj) {
                run = eventList.poll();
                if(run == null) {
                    runingEvent = false;
                }
            }
        }
        return run;
    }

    /**
     * 立即关闭该链接
     */
    public synchronized void closeNow() {
        if(closed) return ;
        closed = true;
        pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_CLOSE, this, context));
        if(readcache != null){
            context.putByteBufferToCache(readcache);/*回收*/
            readcache = null;
        }
    }

    public boolean isClosed(){
        return closed;
    }

    public boolean isWaitForRecvPack(){
        return isWaitForRecvPack;
    }

    public void packWait(int timeout) {
        synchronized (recvPackwait) {
            isWaitForRecvPack = true;
            try {
                recvPackwait.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isWaitForRecvPack = false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Session) {
            return id == ((Session) obj).id;
        }
        return false;
    }

    public void packNotify() {
        synchronized (recvPackwait) {
            isWaitForRecvPack = false;
            recvPackwait.notify();
        }
    }

    public Object getPack() {
        return notifyRecvMsg;
    }

    public void setPack(Object msg) {
        this.notifyRecvMsg = msg;
        packNotifyAll();
    }

    public void packNotifyAll() {
        synchronized (recvPackwait) {
            isWaitForRecvPack = false;
            recvPackwait.notifyAll();
        }
    }
    public abstract SocketAddress getLocaAddress();
    public abstract SocketAddress getRemoteAddress();
    public abstract void write(Object data);
    /**
     * 数据发送完毕后关闭
     */
    public abstract void closeOnFlush();

    @Override
    public String toString() {
        return "[SESSIONID:" + id + ", LocaAddress:" + getLocaAddress() + ", RemoteAddress:" + getRemoteAddress() + "]";
    }
}
