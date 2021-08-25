package lotus.http.server.support;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import lotus.http.WebSocketFrame;
import lotus.nio.LotusIOBuffer;
import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;

public class WebSocketProtocolCodec implements ProtocolCodec{

    /*
     * 参考实现
     * https://tools.ietf.org/html/rfc6455#page-31
     */    
    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
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
        if(msg instanceof HttpMessageWrap) {
            out.append((ByteBuffer) ((HttpMessageWrap) msg).data);
            return true;
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
            out.append(b1);
            out.append(b2);
        }else if(datalen < 65535) {
            b2 = (byte) (b2 | 126);
            //发送2b长度
            out.append(b1);
            out.append(b2);
            out.append((byte) (datalen >>> 8));
            out.append((byte) (datalen & 0xff));
        }else {
            b2 = (byte) (b2 | 127);
            //发送8b长度
            out.append(b1);
            out.append(b2);
            out.append((byte) (datalen & 0xff));
            out.append((byte) ((datalen >>> 8) & 0xff));
            out.append((byte) ((datalen >>> 16) & 0xff));
            out.append((byte) ((datalen >>> 24) & 0xff));
            out.append((byte) ((datalen >>> 32) & 0xff));
            out.append((byte) ((datalen >>> 40) & 0xff));
            out.append((byte) ((datalen >>> 48) & 0xff));
            out.append((byte) ((datalen >>> 56) & 0xff));
        }
        if(frame.mask != null) {
            out.append(frame.mask);
        }
        
        if(datalen > 0) {
            if(frame.masked) {
                int pLen = frame.body.length;
                for(int i = 0; i < pLen; i++){
                    frame.body[i] = (byte) (frame.body[i] ^ frame.mask[i % 4]);
                }
            }
            out.append(frame.body);
        }
     
        return true;
    }

}
