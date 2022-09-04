package lotus.http.server.support;

import java.nio.ByteBuffer;

import lotus.http.server.HttpServer;
import lotus.http.server.support.SSLState.SelfHandhakeState;
import lotus.nio.LotusIOBuffer;
import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpSession;
import lotus.utils.Utils;

public class HttpsProtocolCodec implements ProtocolCodec{
    // 19 -- 25
    //private final static byte       HTTPS_REQ_START         =   0x16;
    
    private HttpServer                      context                 =   null;
    private HttpProtocolCodec               httpProtocolCodec       =   null;
    
    public HttpsProtocolCodec(HttpServer context) {
        this.context = context;
        httpProtocolCodec = context.getHttpProtocolCodec();
    }
    
    /***
     * 处理握手
     * @param session
     * @return 如果握手完成后有多余的数据则返回, 会流转到decode处理
     * @throws Exception 触发异常则会关闭此连接
     */
    public ByteBuffer doHandshake(NioTcpSession session) throws Exception {
        /*
         * https://www.cnblogs.com/LittleHann/p/3733469.html?utm_source=tuicool&utm_medium=referral
         * tls/ssl 协议起始字节
         * 1) CHANGE_CIPHER_SPEC        20     0x14
         * 2) ALERT                     21     0x15
         * 3) HANDSHAKE                 22     0x16
         * 4) APPLICATION_DATA          23     0x17
         * */
        ByteBuffer tmpBuf = session.getWriteCacheBuffer(0);
        int n = session.read(tmpBuf);
        if(n < 0) {
            //已关闭
            return null;
        }
        tmpBuf.flip();
        tmpBuf.mark();
        byte begin = tmpBuf.get();
        if(begin > 19 && begin < 25) {//是否https
            
        }
        tmpBuf.reset();
        return tmpBuf;
    }
    
    
    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {

        ByteBuffer outBuffer = null;
        if(context.isEnableSSL() && in.remaining() > 0) {
            
           
            in.mark();
            System.out.println("data:" + Utils.byte2hex(in.array(), in.limit()));

            in.reset();
            in.mark();
            byte begin = in.get();
            in.reset();
            if(begin > 19 && begin < 25) {
                SSLState state = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
                if(state == null) {
                    state = new SSLState(context, (NioTcpSession) session);
                    session.setAttr(SSLState.SSL_STATE_KEY, state);
                }
                if(state.isHandshaked()) {
                    outBuffer = session.getWriteCacheBuffer(in.capacity());
                    state.unwrap(in, outBuffer);
                } else {
                    ByteBuffer surplus = session.getWriteCacheBuffer(in.capacity());
                    SelfHandhakeState res = state.doHandshake(in, surplus);
                    switch(res) {
                        case NEED_DATA:
                            //数据不够
                            return false;
                        case NEED_SEND:
                            return true;
                        case FINISHED:
                            surplus.flip();
                            if(surplus.hasRemaining()) {
                                //握手完成, 但是还有数据
                                System.out.println("xxxx->" + surplus.toString());
                                outBuffer = session.getWriteCacheBuffer(surplus.capacity());
                                state.unwrap(surplus, outBuffer);
                                break;
                            }
                            return true;
                    }
                }
               
            } else {
                outBuffer = in;
            }
        } else {
            outBuffer = in;
        }
        boolean r = httpProtocolCodec.decode(session, outBuffer, out);
        System.out.println("http ok");
        return r;
    }

   
    
    @Override
    public boolean encode(Session session, Object msg, LotusIOBuffer out) throws Exception {
        HttpMessageWrap wrap = (HttpMessageWrap) msg;
        if(wrap.type == HttpMessageWrap.HTTP_MESSAGE_HTTPS_HANDHAKE) {
            out.append((ByteBuffer) wrap.data);
            return true;
        }
        if(context.isEnableSSL()) {
            //启用ssl时并且使用ssl协议访问
            LotusIOBuffer tmpOut = new LotusIOBuffer(session.getContext());
            boolean r = httpProtocolCodec.encode(session, msg, tmpOut);
            SSLState state = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
            if(state != null) {
                ByteBuffer[] bufs = tmpOut.getAllBuffer();
                ByteBuffer outBuf;
                for(ByteBuffer buf : bufs) {
                    outBuf = session.getWriteCacheBuffer(buf.capacity());
                    buf.flip();
                    state.wrap(buf, outBuf);
                    out.append(outBuf);
                }
                tmpOut.free();
                return r;
            }
           
        } 
        return httpProtocolCodec.encode(session, msg, out);
    }

}
