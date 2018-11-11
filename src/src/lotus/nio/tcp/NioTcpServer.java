package lotus.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

import lotus.nio.IoHandler;
import lotus.nio.NioContext;
import lotus.utils.Utils;

public class NioTcpServer extends NioContext{
	private ServerSocketChannel ssc			=	null;
	private NioTcpIoProcess     ioprocess[] =   null;
    private int                 iipBound    =   0;
    private final ReentrantLock rliplock    =   new ReentrantLock();
	private AcceptThread        acceptrhread=   null;
	private long                idcount     =   0l;
	
    public NioTcpServer(){
		super();

	}

    public void start(InetSocketAddress addr) throws IOException {
        
        if(this.handler == null) {
            this.handler = new IoHandler() { };
        }
        
        this.bufferlist = new LinkedBlockingQueue<ByteBuffer>(buffer_list_length);
        
        ssc = ServerSocketChannel.open();
        ssc.socket().setReceiveBufferSize(buff_read_cache_size);
        ssc.configureBlocking(false);
        ioprocess = new NioTcpIoProcess[selector_thread_total];
        
        for(int i = 0; i < selector_thread_total; i++){
            ioprocess[i] = new NioTcpIoProcess(this);
            new Thread(ioprocess[i], "lotus nio tcp server selector thread - " + i).start();
        }
        
        if(event_pool_thread_size > 0) {
            this.executor = Executors.newFixedThreadPool(event_pool_thread_size, new ThreadFactory() {
                
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "lotus nio tcp server default fixed thread pool");
                }
            });
        }
        
        /*一个线程 accept */
        acceptrhread = new AcceptThread();
        new Thread(acceptrhread, "lotus nio tcp accept thread").start();
        
        ssc.bind(addr);
    }

	
	public void close() {
		try {
            acceptrhread.close();
            if(executor != null) executor = null;
		    for(int i = 0; i < selector_thread_total; i++){
		        try {
		            ioprocess[i].close();
                } catch (Exception e) { }
		    }
            bufferlist.clear();
		    if(ssc != null) ssc.close();
		    ssc = null;
        } catch (IOException e) {}
	}
	
    private class AcceptThread implements Runnable{
        Selector mslAccept = null;
        volatile boolean run = true;
        public AcceptThread() throws IOException {
            mslAccept = Selector.open();
        }
        
        public void close(){
            run = false;
            mslAccept.wakeup();
        }
        
        @Override
        public void run() {
            try {
                ssc.register(mslAccept, SelectionKey.OP_ACCEPT);
                while(mslAccept != null && run){
                    try {
                        if(mslAccept.select(SELECT_TIMEOUT) == 0){
                            
                            if(!run) break;
                            
                            Utils.SLEEP(1);
                            continue;
                        }
                        Iterator<SelectionKey> keys = mslAccept.selectedKeys().iterator();
                        
                        while(keys.hasNext()){
                            SelectionKey key = keys.next();
                            keys.remove();
                            if(key.isAcceptable()){
//                                long s = System.currentTimeMillis();
                                SocketChannel client = ssc.accept();
                                if(client == null) continue;
                                client.configureBlocking(false);
                                client.socket().setSoTimeout(socket_time_out);
                                client.socket().setReceiveBufferSize(buff_read_cache_size);
                                client.socket().setSendBufferSize(buff_read_cache_size);
                                client.finishConnect();
                                rliplock.lock();/*排队*/
                                try {
                                    iipBound ++;/**/
                                    if(iipBound >= ioprocess.length){
                                        iipBound = 0;
                                    }
                                    ioprocess[iipBound].putChannel(client, ++idcount);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }finally{
                                    rliplock.unlock();
                                }
                                if(idcount >= Long.MAX_VALUE){
                                    idcount = 0l;
                                }

//                                System.out.println("连接开始:" + s + " 处理连接用时:" + (System.currentTimeMillis() - s));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 
                    
                }
                mslAccept.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	
}
