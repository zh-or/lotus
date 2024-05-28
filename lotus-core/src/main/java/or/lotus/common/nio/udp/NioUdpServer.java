package or.lotus.common.nio.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

import or.lotus.common.nio.IoHandler;
import or.lotus.common.nio.NioContext;

public class NioUdpServer extends NioContext {
    private DatagramChannel dc              =   null;
    private NioUdpIOProcess ioProcess[]     =   null;
    private final ReentrantLock rliplock    =   new ReentrantLock();
    private long                idcount     =   0l;

    public NioUdpServer() {
        super();
    }


    public void start(InetSocketAddress addr) throws IOException  {
        if(this.handler == null) {
            this.handler = new IoHandler() {
            };
        }

        this.bufferlist = new LinkedBlockingQueue<ByteBuffer>(buffer_list_length);

        dc = DatagramChannel.open();
        dc.configureBlocking(false);
        dc.socket().setReceiveBufferSize(buff_cache_size);
        dc.socket().setSendBufferSize(buff_cache_size);

        ioProcess = new NioUdpIOProcess[selector_thread_total];

        for(int i = 0; i < selector_thread_total; i++){
            ioProcess[i] = new NioUdpIOProcess(this);
            new Thread(ioProcess[i], "lotus nio udp server selector thread - " + i).start();
        }

        if(event_pool_thread_size > 0) {
            this.executor = Executors.newFixedThreadPool(event_pool_thread_size, new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "lotus nio udp server default fixed thread pool");
                }
            });
        }

        dc.socket().bind(addr);
    }

}
