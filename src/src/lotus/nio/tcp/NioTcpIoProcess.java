package lotus.nio.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import lotus.nio.IoEventRunnable;
import lotus.nio.IoEventRunnable.IoEventType;
import lotus.nio.IoProcess;
import lotus.nio.NioContext;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;
import lotus.utils.Utils;


public class NioTcpIoProcess extends IoProcess implements Runnable{
	private Selector                    selector    = null;    
    
    public NioTcpIoProcess(NioContext context) throws IOException {
    	super(context);
		selector = Selector.open();
		tmp_buffer = new byte[context.getSessionCacheBufferSize()];
	}
    
    public NioTcpSession putChannel(SocketChannel channel, long id) throws Exception{
        return putChannel(channel, id, true);
    }
    
    public NioTcpSession putChannel(SocketChannel channel, long id, boolean event) throws Exception{
        if(channel == null || selector == null) throw new Exception("null");
        NioTcpSession session = new NioTcpSession(context, channel, this, id);
        SelectionKey key = null;
        if(event){
            /*call on connection*/
            /*register 时会等待 selector.select() 所以此处先唤醒selector以免锁冲突 */
            selector.wakeup();
            key = channel.register(selector, SelectionKey.OP_READ, session);
            
            session.setKey(key);
            selector.wakeup();
            session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CONNECTION, session, context));
        }else{
            /*register 时会等待 selector.select() 所以此处先唤醒selector以免锁冲突 */
            selector.wakeup();
            key = channel.register(selector, SelectionKey.OP_CONNECT, session);
            session.setKey(key);
            selector.wakeup();
        }
        return session;
    }
    
    @Override
    public void run() {
        
        while(isrun && selector != null){
            try {
                if(isessiontimeout != 0){
                    try {
                        handleTimeOut();
                    } catch (Exception e) {}
                }
                handleIoEvent();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
    }
   

	/*缓存buff的状态*/
	private volatile int remaining = 0;
	private volatile int capacity = 0;
	
	private byte[] tmp_buffer;
	
    private void handleIoEvent() throws IOException {
        if(selector.select(NioContext.SELECT_TIMEOUT) == 0){
            if(!isrun) return;
            Utils.SLEEP(1);
            return;
        }
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
       
        while(keys.hasNext()){
            if(!isrun) break;
            
            SelectionKey key = keys.next();
            keys.remove();
            
            NioTcpSession session = (NioTcpSession) key.attachment();
            if(session == null) {
                continue;
            }
            
            try {
                if(!key.isValid()){
                    session.closeNow();
                    continue;
                }
                if(key.isReadable()){/*call decode */
//                    long s = System.currentTimeMillis();
                    ByteBuffer readcache = session.getReadCacheBuffer();
                    int len = session.getChannel().read(readcache);
                    if(len < 0){/*EOF*/
                        session.closeNow();
                        continue;
                    }else{
                        //capacity 容量
                        //limit 也就是缓冲区可以利用（进行读写）的范围的最大值
                        //position 当前读写位置
                        
                        readcache.flip();
                        ProtocolDecoderOutput msgout = session.getProtocolDecoderOutput();
                        boolean ishavepack = false;
                        
                        try {
                            ishavepack = session.getProtocoCodec().decode(session, readcache, msgout);
                          //把未读的数据复制到缓冲区起始位置 此时 position 为数据结尾
                            
                        } catch (Exception e) {
                            session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                        }

                        remaining = readcache.remaining();
                        
                        if(remaining <= 0){//用完了回收掉
                            context.putByteBufferToCache(readcache);
                            session.updateReadCacheBuffer(null);
                        }else{
                            copyData(readcache);
                            if(remaining >= readcache.capacity()){/*已经读满了, 缓存不够. 就不写环形缓冲队列了 :(*/
                                readcache.rewind();//重置 position 位置为 0 并忽略 mark
                                capacity = readcache.capacity();
                                if(capacity > tmp_buffer.length){//扩过容且缓存不够
                                    tmp_buffer = null;
                                    tmp_buffer = new byte[capacity];
                                }
                                readcache.get(tmp_buffer, 0, remaining);
                                int newLen = readcache.capacity() * 2;;
                                context.putByteBufferToCache(readcache);/*回收了*/
                                ByteBuffer newreadcache = ByteBuffer.allocateDirect(newLen);
                                //session.resetCapacity(tmpdata, limit);/*直接扩容 缓存大小设置好点, 就不会有这些蛋疼的问题了*/
                                /*扩容后这便是一个新的obj了, 故手动更新*/
                                newreadcache.put(tmp_buffer, 0, remaining);
                                session.updateReadCacheBuffer(newreadcache);/*update*/
                            }else{
                                /*readcache.position(readcache.limit());
                                readcache.limit(readcache.capacity());*/
                                readcache.compact();
                                session.updateReadCacheBuffer(readcache);/*update*/
                            }
                        }
                        
                        if(ishavepack){
                            session.setLastActive(System.currentTimeMillis());
                            session.pushEventRunnable(new IoEventRunnable(msgout.read(), IoEventType.SESSION_RECVMSG, session, context));
                            msgout.write(null);
                        }
                        selector.wakeup();
                    }
                }
                
                if(key.isWritable()){/*call encode*/
                    Object msg = session.poolMessage();
                    if(msg != null){
                        ByteBuffer out = null;
                        try {
                            //编码解码器放session更好
                            out = session.getProtocoCodec().encode(session, msg);
                        } catch (Exception e) {
                            session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                        }
                        if(out != null){
                            while(out.hasRemaining()) {/*这里最好不要写入超过8k的数据*/
                                session.getChannel().write(out);
                            }
                            out.clear();
                            session.setLastActive(System.currentTimeMillis());
                            /*call message sent*/
                            session.pushEventRunnable(new IoEventRunnable(msg, IoEventType.SESSION_SENT, session, context));
                        }
                        session.setLastActive(System.currentTimeMillis());
                    }else{
                        Object msglock = session.getMessageLock();
                        synchronized (msglock) {
                            if(session.getWriteMessageSize() <= 0){
                                session.removeInterestOps(SelectionKey.OP_WRITE);
                            }
                        }
                        
                        if(session.isSentClose()){
                            session.closeNow();
                        }
                    }
                }
                
                
                if(key.isConnectable()){

                    synchronized (session) {
                        session.notifyAll();
                    }
                    //Finishes the process of connecting a socket channel. 
                    //fuck the doc
                    try {
                        if(((SocketChannel) key.channel()).finishConnect() == false){
                            /*连接失败???*/
                            key.cancel();
                            continue;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    
                    session.removeInterestOps(SelectionKey.OP_CONNECT);//取消连接成功事件
                    session.addInterestOps(SelectionKey.OP_READ);//注册读事件
                    session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CONNECTION, session, context));
                }
            } catch (Exception e) {
                //e.printStackTrace();
                /*call exception*/
                //session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                //e.printStackTrace();
                session.closeNow();
                //cancelKey(key);/*对方关闭了?*/
            } 
        }
    }    
    
    private void handleTimeOut(){
        Iterator<SelectionKey> keys = selector.keys().iterator();
        final long nowtime = System.currentTimeMillis();
        while(keys.hasNext()){
            SelectionKey key = keys.next();/*这里报错?*/
            if(!isrun) break;
            
            if (key.channel() instanceof ServerSocketChannel) {
                continue;
            }
            
            if (key.isValid() == false) {
                continue;
            }
            NioTcpSession session = (NioTcpSession) key.attachment();
            if(session != null && nowtime - session.getLastActive() > isessiontimeout){
                /*call on idle */
                session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_IDLE, session, context));
                session.setLastActive(System.currentTimeMillis());
                
            }
        }
    }
    
    public void cancelKey(SelectionKey key){
        if(key == null) return;
        try {
            Session session = (Session) key.attachment();
            session.closeNow();
            key.channel().close();
            key.cancel();
        } catch (Exception e) {}
    }

    public void close(){
        isrun = false;
        selector.wakeup();
        try {
            Iterator<SelectionKey> it = selector.keys().iterator();
            while(it.hasNext()){
                cancelKey(it.next());
            }
            selector.selectNow();
            selector.close();
            selector.wakeup();
            if(selector != null) selector.close();
        } catch (IOException e) {}
    }

    private void copyData(ByteBuffer buff){
        if(buff.position() > 0){
            remaining = buff.remaining();
            if(remaining > tmp_buffer.length){
                tmp_buffer = new byte[remaining];
            }
            buff.get(tmp_buffer, 0, remaining);
            buff.clear();
            buff.put(tmp_buffer, 0, remaining);
        }
    }
}
