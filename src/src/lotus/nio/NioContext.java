package lotus.nio;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NioContext {
    private static final int        DEF_BUFFER_LIST_MAX_SIZE        =   2048;
    private static final int        DEF_EVENT_THREAD_POOL_SIZE      =   100;
    public static final int         SELECT_TIMEOUT                  =   1000;
    
	protected int                   selector_thread_total           =   0;
    protected int                   session_idle_time               =   0;
    protected int                   socket_time_out                 =   1000 * 10;
    protected int                   buff_read_cache                 =   1024;/*读缓冲区大小*/
    protected int                   buffer_list_max_size            =   0;
    protected IoHandler             handler                         =   null;
    protected ProtocolCodec			procodec						=   null;
    protected ExecutorService       executor_e                      =   null;
    
    protected LinkedBlockingQueue<ByteBuffer> bufferlist            =   null;/*缓存*//*应付像http这样的服务时大量的短连接用*/
    
    public NioContext(){
        selector_thread_total = Runtime.getRuntime().availableProcessors() + 1;/* cpu + 1 */
        this.buffer_list_max_size = DEF_BUFFER_LIST_MAX_SIZE;
        this.executor_e = Executors.newFixedThreadPool(DEF_EVENT_THREAD_POOL_SIZE);
        this.bufferlist = new LinkedBlockingQueue<ByteBuffer>(buffer_list_max_size);
        this.handler = new IoHandler() { };
    }
    

    public NioContext setSelectorThreadTotal(int c){
        this.selector_thread_total = c;
        return this;
    }
    
    public NioContext setReadBufferCacheListSize(int size){
        this.buffer_list_max_size = size;
        this.bufferlist = null;
        this.bufferlist = new LinkedBlockingQueue<ByteBuffer>(buffer_list_max_size);
        return this;
    }
    
    /**
     * 设置事件线程池大小
     * @param size 如果为0则表示不使用单独的线程池
     * @return
     */
    public NioContext setEventThreadPoolSize(int size){
        this.executor_e.shutdown();
        this.executor_e = null;
        if(size > 0) this.executor_e = Executors.newFixedThreadPool(size);
        return this;
    }
    
    /**
     * 如果为 0 则不会检测空闲
     * @param idletime
     * @return
     */
    public NioContext setSessionIdleTime(int idletime){
        this.session_idle_time = idletime;
        return this;
    }
    
    public NioContext setSocketTimeOut(int timeout){
        this.socket_time_out = timeout;
        return this;
    }
    
    public NioContext setSessionCacheBufferSize(int size){
        this.buff_read_cache = size;
        return this;
    }
    
    public int getSessionCacheBufferSize(){
        return this.buff_read_cache;
    }
    
    public NioContext setHandler(IoHandler handler){
        this.handler = handler;
        return this;
    }
    
    public NioContext setEventExecutor(ExecutorService ex){
        this.executor_e.shutdownNow();
        this.executor_e = null;
        this.executor_e = ex;
        return this;
    }

    public NioContext setProtocolCodec(ProtocolCodec codec){
    	this.procodec = codec;
    	return this;
    }
    
    public ProtocolCodec getProtocoCodec(){
    	return this.procodec;
    }
    
    public int getSessionIdleTimeOut(){
        return session_idle_time;
    }
    
    public void ExecuteEvent(Runnable run){
        if(this.executor_e == null){
            run.run();
            return;
        }
    	this.executor_e.execute(run);
    }
    
    public ExecutorService getEventExecutor(){
        return this.executor_e;
    }
    
    public IoHandler getEventHandler(){
        return this.handler;
    }
    
    public ByteBuffer getByteBufferFormCache(){
        ByteBuffer buffer = bufferlist.poll();
        if(buffer == null){
            buffer = ByteBuffer.allocateDirect(buff_read_cache);
            buffer.clear();
        }
        return buffer;
    }
    
    public void putByteBufferToCache(ByteBuffer buffer){
        if(buffer != null && (buffer.capacity() <= buff_read_cache) && (bufferlist.size() < buffer_list_max_size)){
            buffer.clear();
            bufferlist.add(buffer);
        }else{/*丢弃被扩容过的buffer*/
            buffer = null;
        }
    }

    
}
