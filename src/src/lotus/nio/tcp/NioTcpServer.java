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

public class NioTcpServer extends NioContext{
	private ServerSocketChannel ssc			=	null;
	private NioTcpIoProcess     ioprocess[] =   null;
    private int                 iipBound    =   0;
    private final ReentrantLock rliplock    =   new ReentrantLock();
	private AcceptThread        acceptrhread=   null;
	private long                idcount     =   0l;
	
    public NioTcpServer() {
		super();
	}

	public void bind(InetSocketAddress addr) throws IOException {
		ssc = ServerSocketChannel.open();
		ssc.socket().bind(addr);
		ssc.configureBlocking(false);
		/*一个线程 accept */
		acceptrhread = new AcceptThread();
		new Thread(acceptrhread, "lotus nio accept thread").start();
		ioprocess = new NioTcpIoProcess[selector_thread_total];
		
		for(int i = 0; i < selector_thread_total; i++){
		    ioprocess[i] = new NioTcpIoProcess(this);
		    new Thread(ioprocess[i], "lotus nio server selector thread-" + i).start();
		}
	}
	
	public void unbind() {
		try {
		    for(int i = 0; i < selector_thread_total; i++){
		        try {
		            ioprocess[i].close();
                } catch (Exception e) { }
		    }
            executor_e.shutdownNow();
            bufferlist.clear();
            if(acceptrhread != null) acceptrhread.close();
		    if(ssc != null) ssc.close();
		    ssc = null;
        } catch (IOException e) {}
	}
	
    private class AcceptThread implements Runnable{
        Selector mslAccept = null;
        boolean run = true;
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
                                    ioprocess[iipBound].putChannel(client, ++idcount);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }finally{
                                    rliplock.unlock();
                                }
                                if(idcount >= Long.MAX_VALUE){
                                    idcount = 0l;
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
