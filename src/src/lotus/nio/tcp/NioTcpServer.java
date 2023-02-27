package lotus.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lotus.nio.IoHandler;
import lotus.nio.NioContext;
import lotus.utils.Utils;

public class NioTcpServer extends NioContext{
    private ServerSocketChannel ssc			=	null;
    private NioTcpIoProcess     ioprocess[] =   null;
    private int                 iipBound    =   0;
    private final ReentrantLock rliplock    =   new ReentrantLock();
    private AcceptThread        acceptRunner=   null;
    private Thread              acceptThread=   null;
    private long                idcount     =   0l;
    private boolean             tcpNoDelay  =   false;

    public NioTcpServer() {
        super();
    }

    public void start(InetSocketAddress addr) throws IOException {

        if(this.handler == null) {
            this.handler = new IoHandler() { };
        }

        this.bufferlist = new LinkedBlockingQueue<ByteBuffer>(buffer_list_length);

        ssc = ServerSocketChannel.open();
        //不用设置此值, 看起来操作系统会自动优化
        //ssc.socket().setReceiveBufferSize(buff_cache_size);
        ssc.configureBlocking(false);
        ioprocess = new NioTcpIoProcess[selector_thread_total];

        for(int i = 0; i < selector_thread_total; i++) {
            ioprocess[i] = new NioTcpIoProcess(this);
            Thread tmpThread = new Thread(ioprocess[i], "lotus nio tcp server selector thread - " + i);
            ioprocess[i].setThread(tmpThread);
            tmpThread.start();
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
        acceptRunner = new AcceptThread();
        acceptThread = new Thread(acceptRunner, "lotus nio tcp accept thread");
        acceptThread.start();

        ssc.bind(addr);
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean isNoDelay) {
        tcpNoDelay = isNoDelay;
    }

    public void close() {
        try {
            acceptRunner.close();
            acceptThread.join(20000);
            if(executor != null) {
                try {
                    ((ExecutorService) executor).shutdown();
                    ((ExecutorService) executor).awaitTermination(10, TimeUnit.SECONDS);
                } catch(Exception e2) {
                    e2.printStackTrace();
                }
                executor = null;
            }
            for(int i = 0; i < selector_thread_total; i++){
                try {
                    ioprocess[i].close();
                    ioprocess[i].joinThread(20000);
                } catch (Exception e) { }
            }
            bufferlist.clear();
            if(ssc != null) ssc.close();
            ssc = null;
        } catch (Exception e) {}
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
                                if(so_time_out > 0) {
                                    client.socket().setSoTimeout(so_time_out);
                                }
                                //不用设置此值, 看起来操作系统会自动优化
                                //client.socket().setReceiveBufferSize(buff_cache_size);
                                //client.socket().setSendBufferSize(buff_cache_size);
                                if(client.isConnectionPending()) {
                                    client.finishConnect();
                                }
                                rliplock.lock();/*排队*/
                                try {
                                    iipBound ++;/**/
                                    if(iipBound >= ioprocess.length) {
                                        iipBound = 0;
                                    }
                                    if(tcpNoDelay) {
                                        client.socket().setTcpNoDelay(true);
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
