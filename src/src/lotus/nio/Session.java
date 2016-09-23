package lotus.nio;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import lotus.nio.IoEventRunnable.IoEventType;

public abstract class Session {
    protected ConcurrentHashMap<Object, Object> attrs           =   null;
    protected NioContext                        context     	=   null;
    protected long					            lastactive		=	0l;
    protected volatile ByteBuffer               readcache       =   null;
    protected volatile ByteBuffer               writecache      =   null;
    protected LinkedBlockingQueue<Runnable>     eventlist       =   null;
    protected volatile boolean                  runingevent     =   false;
    protected ProtocolDecoderOutput             deout           =   null;
    protected long                              id              =   0l;
    protected final Object                      recvPackwait    =   new Object();
    protected long                              createtime      =   0l;
    protected volatile boolean                  closed          = false;
    
    public Session (NioContext context, long id){
        this.context = context;
        this.attrs = new ConcurrentHashMap<Object, Object>();
        this.id = id;
        this.createtime = System.currentTimeMillis();
        setLastActive(System.currentTimeMillis());
        readcache = context.getByteBufferFormCache();
        writecache = context.getByteBufferFormCache();
        eventlist = new LinkedBlockingQueue<Runnable>();
        deout = new ProtocolDecoderOutput();
    }
    
    public long getId(){
        return id;
    }
    
    public long getCreateTime(){
        return createtime;
    }
    
    public Object getAttr(Object key){
    	return getAttr(key, null);
    }
    
    public abstract int getWriteMessageSize();
    
    public Object getAttr(Object key, Object defval){
    	Object val = attrs.get(key);
    	if(val == null) return defval;
    	return val;
    }
    
    public void setAttr(Object key, Object val){
    	attrs.put(key, val);
    }
    
    public Object removeAttr(Object key){
        return attrs.remove(key);
    }
    
    public void setLastActive(long t){
    	this.lastactive = t;
    }
    
    public long getLastActive(){
    	return this.lastactive;
    }
    
    public ProtocolDecoderOutput getProtocolDecoderOutput(){
        return deout;
    }

    public ByteBuffer getReadCacheBuffer(){
    	return readcache;
    }
    
    public ByteBuffer getWriteCacheBuffer(int size){
        if(writecache.capacity() < size){
            context.putByteBufferToCache(writecache);
            this.writecache = null;
            writecache = ByteBuffer.allocate(size);
        }
        return writecache;
    }

    public void updateReadCacheBuffer(ByteBuffer buffer){
        this.readcache = buffer;
    }
    
    public void updateWriteCacheBuffer(ByteBuffer buffer){
        this.writecache = buffer;
    }
    
    public boolean IsRuningEvent(){
        return runingevent;
    }
    
    public void RuningEvent(boolean is){
        this.runingevent = is;
    }
    
    public void pushEventRunnable(Runnable run){
        if(!IsRuningEvent()){
            context.ExecuteEvent(run);
        }else{
            eventlist.add(run);
        }
    }
    
    public Runnable pullEventRunnable(){
        return eventlist.poll();
    }
    
    /**
     * 立即关闭该链接
     */
    public void closeNow(){
        if(closed) return ;
        closed = true;
        context.putByteBufferToCache(readcache);/*回收*/
        context.putByteBufferToCache(writecache);
        pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CLOSE, this, context));
        readcache = null;
    }
    
    public boolean isClosed(){
        return closed;
    }

    public void _wait(int timeout){
        synchronized (recvPackwait) {
            try {
                recvPackwait.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void _notify(){
        synchronized (recvPackwait) {
            recvPackwait.notify();
        }
    }
    
    public void _notifyAll(){
        synchronized (recvPackwait) {
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
