package lotus.nio;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Session {
    protected ConcurrentHashMap<Object, Object> attrs           =   null;
    protected NioContext                        context     	=   null;
    protected long					            lastactive		=	0l;
    protected volatile ByteBuffer               readcache       =   null;
    protected LinkedBlockingQueue<Runnable>     eventlist       =   null;
    protected volatile boolean                  runingevent     =   false;
    protected ProtocolDecoderOutput             deout           =   null;
    
    public Session (NioContext context){
        this.context = context;
        this.attrs = new ConcurrentHashMap<Object, Object>();
        setLastActive(System.currentTimeMillis());
        readcache = context.getByteBufferFormCache();
        eventlist = new LinkedBlockingQueue<Runnable>();
        deout = new ProtocolDecoderOutput();
    }
    
    public Object getAttr(Object key){
    	return getAttr(key, null);
    }
    
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
    
    public Runnable poolEventRunnable(){
        return eventlist.poll();
    }
    
    /**
     * 立即关闭该链接
     */
    public void closeNow(){
        context.putByteBufferToCache(readcache);/*回收*/
    }
    
    public void resetCapacity(byte[] data, int size){
        data = Arrays.copyOf(data, size);
        readcache = ByteBuffer.wrap(data);
    }
    
    public abstract SocketAddress getRemoteAddress();
    public abstract void write(Object data);
    /**
     * 数据发送完毕后关闭
     */
    public abstract void closeOnFlush();
}
