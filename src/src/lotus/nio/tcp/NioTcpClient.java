package lotus.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

import lotus.nio.NioContext;
import lotus.nio.ProtocolCodec;
import lotus.nio.Session;

public class NioTcpClient extends NioContext{
    
    private NioTcpIoProcess     ioprocess[]         =   null;
    private long                idcount             =   0l;
    private int                 iipBound            =   0;
    private final ReentrantLock rliplock            =   new ReentrantLock();
    
    public NioTcpClient(ProtocolCodec codec){
        this.procodec = codec;
    }
    
    public void init() throws IOException{
        ioprocess  = new NioTcpIoProcess[selector_thread_total];
        for(int i = 0; i < selector_thread_total; i++){
            ioprocess[i] = new NioTcpIoProcess(this);
            new Thread(ioprocess[i], "lotus nio selector thread-" + i).start();
        }
    }
    
    public void stop(){
        for(int i = 0; i < selector_thread_total; i++){
            ioprocess[i].close();
        }
    }
    
    public Session connection(InetSocketAddress address){
        return connection(address, 0);
    }
    
    /**
     * 
     * @param address
     * @param timeout 如果此参数不为0, 则表示为同步连接 
     * @return 如果设置了timeout则连接成功返回session, 否则直接返回null
     */
    public Session connection(InetSocketAddress address, int timeout){
        SocketChannel sc;
        NioTcpSession session = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(address);
            //sc.finishConnect();/*检测是否已连接完成*/
            rliplock.lock();
            try {
                iipBound++;
                if(iipBound >= ioprocess.length){
                    iipBound = 0;
                }
                session = ioprocess[iipBound].putChannel(sc, ++idcount, false);
            } catch (Exception e) {
                e.printStackTrace();
            }finally{
                rliplock.unlock();
            }
            if(idcount >= Long.MAX_VALUE){
                idcount = 0l;
            }
            if(timeout > 0 && session != null){
                try {
                    synchronized (session) {
                        session.wait(timeout);
                    }
                } catch (InterruptedException e) {}
                if(sc.finishConnect()){
                    return session;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
}
