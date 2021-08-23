package lotus.nio;

import java.net.SocketException;
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NioContext {
    private static final int        DEF_BUFFER_LIST_MAX_SIZE        =   1024;
    public static final int         SELECT_TIMEOUT                  =   10000;
    
	protected int                   selector_thread_total           =   0;
    protected int                   session_idle_time               =   0;

    protected int                   so_time_out                     =   1000 * 10;
    protected int                   buff_read_cache_size            =   1024;/*读缓冲区大小*/
    protected int                   buffer_list_length              =   0;/*缓存链表最大长度*/
    protected int                   event_pool_thread_size          =   0;
    protected boolean               use_direct_buffer               =   false;//使用
    
    protected IoHandler             handler                         =   null;
    protected ProtocolCodec			procodec						=   null;
    protected Executor              executor                        =   null;
    
    
    protected LinkedBlockingQueue<ByteBuffer> bufferlist            =   null;/*缓存*//*应付像http这样的服务时大量的短连接用*/
    
    public NioContext(){
        this.selector_thread_total = Runtime.getRuntime().availableProcessors() + 1;/* cpu + 1 */
        this.buffer_list_length = DEF_BUFFER_LIST_MAX_SIZE;
    }
    

    public NioContext setSelectorThreadTotal(int c){
        this.selector_thread_total = c;
        return this;
    }
    
    public NioContext setReadBufferCacheListSize(int size){
        this.buffer_list_length = size;
        return this;
    }
    
    /**
     * 分配bytebuffer时是否使用系统的内存
     * @param isUse true 时分配系统内存， false 时分配jvm内存
     * @return
     */
    public NioContext setUseDirectBuffer(boolean isUse) {
        this.use_direct_buffer = isUse;
        return this;
    }
    
    /**
     * 设置事件线程池大小, strart 后调用无效
     * @param size 如果为0则表示不使用单独的线程池
     * @return
     */
    public NioContext setEventThreadPoolSize(int size){
        this.event_pool_thread_size = size;
        return this;
    }
    
    /**
     * 如果为 0 则不会检测空闲
     * @param idletime 毫秒
     * @return
     */
    public NioContext setSessionIdleTime(int idletime){
        this.session_idle_time = idletime;
        return this;
    }
    
    /**
     *  Enable/disable {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}
     *  with the specified timeout, in milliseconds. With this option set
     *  to a non-zero timeout, a read() call on the InputStream associated with
     *  this Socket will block for only this amount of time.  If the timeout
     *  expires, a <B>java.net.SocketTimeoutException</B> is raised, though the
     *  Socket is still valid. The option <B>must</B> be enabled
     *  prior to entering the blocking operation to have effect. The
     *  timeout must be {@code > 0}.
     *  A timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeout the specified timeout, in milliseconds.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error.
     * @since   JDK 1.1
     * @see #getSoTimeout()
     */
    public NioContext setSoTimeOut(int timeout){
        this.so_time_out = timeout;
        return this;
    }
    
    public NioContext setSessionCacheBufferSize(int size){
        this.buff_read_cache_size = size;
        return this;
    }
    
    public int getSessionCacheBufferSize(){
        return this.buff_read_cache_size;
    }
    
    public boolean isUseDirectBuffer() {
        return this.use_direct_buffer;
    }
    
    
    public NioContext setHandler(IoHandler handler){
        this.handler = handler;
        return this;
    }
    
    public NioContext setEventExecutor(Executor ex){
        if(this.executor != null) {
            this.executor = null;
        }
        this.executor = ex;
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
        if(this.executor == null){
            run.run();
        }else {
            this.executor.execute(run);
        }
    }
    
    public Executor getEventExecutor(){
        return this.executor;
    }
    
    public IoHandler getEventHandler(){
        return this.handler;
    }
    
    public ByteBuffer getByteBufferFormCache() {
        return getByteBufferFormCache(buff_read_cache_size);
    }
    
    public ByteBuffer getByteBufferFormCache(int size) {
        ByteBuffer buffer = null;
        if(size <= buff_read_cache_size) {
            size = buff_read_cache_size;
            buffer = bufferlist.poll();
        }
        
        if(buffer == null){
            if(use_direct_buffer) {
                buffer = ByteBuffer.allocateDirect(size);
            }else {
                buffer = ByteBuffer.allocate(size);
            }
        }
        return buffer;
    }
    
    public void putByteBufferToCache(ByteBuffer buffer){
        if(buffer != null && (buffer.capacity() == buff_read_cache_size) && (bufferlist.size() < buffer_list_length)){
            buffer.clear();
            bufferlist.add(buffer);
        }else{/*丢弃被扩容过的buffer*/
            
            buffer = null;
        }
    }
    
}
