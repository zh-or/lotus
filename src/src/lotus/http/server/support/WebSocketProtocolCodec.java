package lotus.http.server.support;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import lotus.http.WebSocketFrame;
import lotus.http.server.HttpServer;
import lotus.nio.LotusIOBuffer;
import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;

public class WebSocketProtocolCodec implements ProtocolCodec{
    private HttpServer   context                =   null;
    
    public WebSocketProtocolCodec(HttpServer context) {
        this.context = context;
    }
    
    /*
     * 参考实现
     * https://tools.ietf.org/html/rfc6455#page-31
     */    
    @Override
    public boolean decode(Session session, ByteBuffer netIn, ProtocolDecoderOutput out) throws Exception {
        ByteBuffer in;
        SSLState state = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
        if(context.isEnableSSL() && state != null) {
            in = session.getWriteCacheBuffer(netIn.capacity());
            if(!state.unwrap(netIn, in)) {
                session.putWriteCacheBuffer(in);
                return false;
            }
            in.flip();
        } else {
            in = netIn;
        }
        
        int  remaining = in.remaining();
        long packlen   = 0;//所需包最小长度
        int  headLen   = 2;
        if(remaining < headLen){
            return false;
        }
        in.mark();
        byte b = in.get();
        WebSocketFrame frame = new WebSocketFrame((byte) (b & 0x0f));
        frame.fin = (b & 0x80) != 0;
        frame.rsv1 = (b & 0x40) != 0;
        frame.rsv2 = (b & 0x20) != 0;
        frame.rsv3 = (b & 0x10) != 0;
     
        b = in.get();//第一位是mask, 后面是playloadlength

        frame.masked = (b & 0x80) != 0;
        frame.payload = b & 0x7f;
        
        if(frame.payload == 126) {
            headLen += 2;
            if(remaining < headLen){
                in.reset();
               return false;
            }
            byte[] bytes = new byte[2];
            in.get(bytes);
            packlen = new BigInteger(bytes).intValue();
        } else if(frame.payload == 127) {
            headLen += 8;
            if(remaining < headLen){//127读8个字节,后8个字节为payloadLength
                in.reset();
                return false;
            }
            byte[] bytes = new byte[8];
            in.get(bytes);
            packlen = new BigInteger(bytes).longValue();
        } else {
            packlen = frame.payload;
        }

        if(remaining < packlen + headLen) {
            in.reset();
            return false;
        }
        
        if(frame.masked) {
            frame.mask = new byte[4];
            in.get(frame.mask);
        }
        if(packlen > 0) {
            
            frame.body = new byte[(int) packlen];
            in.get(frame.body);
        }
  
        out.write(frame);
        return true;
    }

    @Override
    public boolean encode(Session session, Object msg, LotusIOBuffer out) throws Exception {
        SSLState state = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
        
        
        if(msg instanceof HttpMessageWrap) {
            //http升级到websocket时, 返回http头
            HttpMessageWrap httpMsgWrap = (HttpMessageWrap) msg;
            
            ByteBuffer outBuffer = (ByteBuffer) httpMsgWrap.data;
           
            if(context.isEnableSSL() && state != null) {
                ByteBuffer netOutBuffer = session.getWriteCacheBuffer(outBuffer.capacity());
                outBuffer.flip();
                state.wrap(outBuffer, netOutBuffer);
                out.append(netOutBuffer);
                session.putWriteCacheBuffer(outBuffer);
            } else {

                out.append(outBuffer);
            }
            
            return true;
        }

        LotusIOBuffer tmpOut;
        if(context.isEnableSSL() && state != null) {
            tmpOut = new LotusIOBuffer(session.getContext());
        } else {
            tmpOut = out;
        }
        
        
        WebSocketFrame frame   = (WebSocketFrame) msg;
        int            datalen = (frame.body != null ? frame.body.length : 0);
        
        byte b1 = 
                (byte)( (frame.fin  ? 0x80 : 0x00) | 
                        (frame.rsv1 ? 0x40 : 0x00) |
                        (frame.rsv2 ? 0x20 : 0x00) |
                        (frame.rsv3 ? 0x10 : 0x00)
                       );
        b1 = (byte) (b1 | (0x0f & frame.opcode));
        byte b2 = (byte) (frame.masked ? 0x80 : 0x00);
        
        if(datalen < 126) {
            b2 = (byte) (b2 | datalen);
            tmpOut.append(b1);
            tmpOut.append(b2);
        }else if(datalen < 65535) {
            b2 = (byte) (b2 | 126);
            //发送2b长度
            tmpOut.append(b1);
            tmpOut.append(b2);
            tmpOut.append((byte) (datalen >>> 8));
            tmpOut.append((byte) (datalen & 0xff));
        }else {
            b2 = (byte) (b2 | 127);
            //发送8b长度
            tmpOut.append(b1);
            tmpOut.append(b2);
            tmpOut.append((byte) (datalen & 0xff));
            tmpOut.append((byte) ((datalen >>> 8) & 0xff));
            tmpOut.append((byte) ((datalen >>> 16) & 0xff));
            tmpOut.append((byte) ((datalen >>> 24) & 0xff));
            tmpOut.append((byte) ((datalen >>> 32) & 0xff));
            tmpOut.append((byte) ((datalen >>> 40) & 0xff));
            tmpOut.append((byte) ((datalen >>> 48) & 0xff));
            tmpOut.append((byte) ((datalen >>> 56) & 0xff));
        }
        if(frame.mask != null) {
            tmpOut.append(frame.mask);
        }
        
        if(datalen > 0) {
            if(frame.masked) {
                int pLen = frame.body.length;
                for(int i = 0; i < pLen; i++) {
                    frame.body[i] = (byte) (frame.body[i] ^ frame.mask[i % 4]);
                }
            }
            tmpOut.append(frame.body);
        }
        if(context.isEnableSSL() && state != null) {
            ByteBuffer[] bufs = tmpOut.getAllBuffer();
            ByteBuffer outBuf;
            for(ByteBuffer buf : bufs) {
                outBuf = session.getWriteCacheBuffer(buf.capacity());
                buf.flip();
                state.wrap(buf, outBuf);
                out.append(outBuf);
            }
            tmpOut.free();
        } 
        return true;
    }

}
