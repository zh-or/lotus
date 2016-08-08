package lotus.nio.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;

import lotus.nio.IoEventRunnable;
import lotus.nio.IoEventRunnable.IoEventType;
import lotus.nio.IoProcess;
import lotus.nio.NioContext;
import lotus.nio.ProtocolDecoderOutput;
import lotus.util.Util;


public class NioTcpIoProcess extends IoProcess implements Runnable{
	private Selector                    selector    = null;    
    
    public NioTcpIoProcess(NioContext context) throws IOException {
    	super(context);
		selector = Selector.open();
		tmp_buffer = new byte[context.getSessionReadBufferSize()];
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
            session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CONNECTION, session, context));
            key = channel.register(selector, SelectionKey.OP_READ, session);
        }else{
            key = channel.register(selector, SelectionKey.OP_CONNECT, session);
        }
        
        session.setKey(key);
        return session;
    }
    
    @Override
    public void run() {
        
        while(isrun && selector != null){
            try {
                if(isessiontimeout != 0){
                    handleTimeOut();
                }
                handleIoEvent();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
    }
   

	/*缓存buff的状态*/
	private volatile int position = 0;
	private volatile int limit = 0;
	private volatile int remaining = 0;
	private byte[] tmp_buffer;
	
    private void handleIoEvent() throws IOException {
        if(selector.select(NioContext.SELECT_TIMEOUT) == 0){
            if(!isrun) return;
            Util.SLEEP(1);
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
                /*if(!key.isValid()){//没有准备好?
                    
                }*/
                
                if(key.isConnectable()){
                    synchronized (session) {
                        session.notifyAll();
                    }
                    session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CONNECTION, session, context));
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));//取消连接成功事件
                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);//注册读事件
                }
                
                if(key.isReadable()){/*call decode */
                    ByteBuffer readcache = session.getReadCacheBuffer();
                    int len = session.getChannel().read(readcache);
                    if(len < 0){/*EOF*/
                        session.closeNow();
                        continue;
                    }else{
                      //readcache.mark();
                        position = readcache.position();
                        limit = readcache.limit();
                        readcache.flip();
                        ProtocolDecoderOutput msgout = session.getProtocolDecoderOutput();
                        boolean ishavepack = false;
                        
                        try {
                            ishavepack = codec.decode(session, readcache, msgout);
                        } catch (Exception e) {
                            session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                        }
                        
                        if(ishavepack){
                            session.setLastActive(System.currentTimeMillis());
                            //readcache.clear();/*直接清空缓存有问题, 如若沾包的话, 后面的也被清空了 */
                            remaining = readcache.remaining();
                            limit = tmp_buffer.length;
                            if(limit >= remaining){
                                readcache.get(tmp_buffer, 0, remaining);/*把剩余的数据弄出来?*/
                                readcache.clear();
                                readcache.put(tmp_buffer, 0, remaining);
                            }else{/*此buff是经过扩容的*//*判断一下 如果扩过容的话, tmp_buff 可能不够存*/
                            	byte[] tmp_exbuff = new byte[remaining];
                            	readcache.get(tmp_exbuff, 0, remaining);
                            	readcache.clear();
                            	readcache.put(tmp_exbuff, 0, remaining);
                            }
                            session.pushEventRunnable(new IoEventRunnable(msgout.read(), IoEventType.SESSION_RECVMSG, session, context));
                            msgout.write(null);
                        }else{
                            readcache.position(position);
                            readcache.limit(limit);
                            if(position >= readcache.capacity()){/*已经读满了, 缓存不够. 就不写环形缓冲队列了 :(*/
                            //    System.out.println("不够?, 当前大小:" + readcache.capacity());
                                byte[] tmpdata = readcache.array();
                                limit = readcache.capacity() * 2;
                                tmpdata = Arrays.copyOf(tmpdata, limit);
                                readcache.clear();
                                context.putByteBufferToCache(readcache);/*回收了*/
                                readcache = ByteBuffer.wrap(tmpdata);
                                //session.resetCapacity(tmpdata, limit);/*直接扩容 缓存大小设置好点, 就不会有这些蛋疼的问题了*/
                                /*扩容后这便是一个新的obj了, 故手动更新*/
                                readcache.position(position);
                                readcache.limit(limit);
                                session.updateReadCacheBuffer(readcache);/*update*/
                            }
                        }
                    }
                }
                
                if(key.isWritable()){/*call encode*/
                    Object msg = session.poolMessage();
                    if(msg != null){
                        ByteBuffer out = null;
                        try {
                            out = codec.encode(session, msg);
                        } catch (Exception e) {
                            session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                        }
                        if(out != null){
                            while(out.hasRemaining()) {
                                session.getChannel().write(out);
                            }
                            session.setLastActive(System.currentTimeMillis());
                            /*call message sent*/
                            session.pushEventRunnable(new IoEventRunnable(msg, IoEventType.SESSION_SENT, session, context));
                        }
                        session.setLastActive(System.currentTimeMillis());
                    }else{
                        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));/*取消写事件*/
                        if(session.isSentClose()){
                            session.closeNow();
                        }
                    }
                }
            } catch (Exception e) {
                /*call exception*/
                //session.pushEventRunnable(new IoEventRunnable(e, IoEventType.SESSION_EXCEPTION, session, context));
                session.closeNow();
                cancelKey(key);/*对方关闭了?*/
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
    
    public void cancelSession(NioTcpSession session){
        /*call close*/
        session.pushEventRunnable(new IoEventRunnable(null, IoEventType.SESSION_CLOSE, session, context));
    }
    
    private void cancelKey(SelectionKey key){
        try {
            key.channel().close();
            key.cancel();
        } catch (Exception e) {}
    }

    public void close(){
        isrun = false;
        try {
            selector.wakeup();
            if(selector != null) selector.close();
        } catch (IOException e) {}
    }

}
