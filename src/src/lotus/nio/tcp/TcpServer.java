package lotus.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import lotus.nio.NioContext;
import lotus.util.Util;

public class TcpServer extends NioContext{
	private ServerSocketChannel ssc			=	null;
	private volatile boolean    brun        =   false;
	private TcpIoProcess        ioprocess[] =   null;
    private int                 iipBound    =   0;
    private final ReentrantLock rliplock    =   new ReentrantLock();
	private AcceptThread        acceptrhread=   null;
    public TcpServer() {
		this(0, 0, 0);
	}

	public TcpServer(int selector_thread_total, int expoolsize, int buffer_list_maxsize) {
		super(selector_thread_total, expoolsize, buffer_list_maxsize);
	}

	public void bind(InetSocketAddress addr) throws IOException {
		ssc = ServerSocketChannel.open();
		ssc.socket().bind(addr);
		ssc.configureBlocking(false);
		brun = true;
		/*一个线程 accept */
		acceptrhread = new AcceptThread();
		new Thread(acceptrhread, "lotus nio accept thread").start();
		ioprocess = new TcpIoProcess[selector_thread_total];
		
		for(int i = 0; i < selector_thread_total; i++){
		    ioprocess[i] = new TcpIoProcess(this);
		    new Thread(ioprocess[i], "lotus nio selector thread-" + i).start();
		}
	}
	
	@Override
	public void unbind() {
		try {
		    for(int i = 0; i < selector_thread_total; i++){
		        try {
		            ioprocess[i].close();
                } catch (Exception e) { }
		    }
            executor_e.shutdownNow();
            bufferlist.clear();
            acceptrhread.close();
		    ssc.close();
        } catch (IOException e) {}
	}
	   
    private class AcceptThread implements Runnable{
        Selector mslAccept = null;
        
        public AcceptThread() throws IOException {
            mslAccept = Selector.open();
        }
        
        public void close(){
            mslAccept.wakeup();
        }
        
        @Override
        public void run() {
            try {
                ssc.register(mslAccept, SelectionKey.OP_ACCEPT);
                while(mslAccept != null && brun){
                    try {
                        if(mslAccept.select(SELECT_TIMEOUT) == 0){
                            
                            if(!brun) break;
                            
                            Util.SLEEP(1);
                            continue;
                        }
                        Iterator<SelectionKey> keys = mslAccept.selectedKeys().iterator();
                        
                        while(keys.hasNext()){
                            SelectionKey key = keys.next();
                            keys.remove();
                            if(key.isAcceptable()){
                                SocketChannel client = ssc.accept();
                                if(client == null) continue;
                                client.configureBlocking(false);
                                client.socket().setSoTimeout(socket_time_out);
                                rliplock.lock();/*排队*/
                                try {
                                    iipBound ++;/*不需要什么高端的算法*/
                                    if(iipBound >= ioprocess.length){
                                        iipBound = 0;
                                    }
                                    ioprocess[iipBound].putChannel(client);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }finally{
                                    rliplock.unlock();
                                }
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
