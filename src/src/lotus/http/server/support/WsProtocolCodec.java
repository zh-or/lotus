package lotus.http.server.support;

import java.nio.ByteBuffer;

import lotus.http.server.WsRequest;
import lotus.http.server.WsResponse;
import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;

public class WsProtocolCodec implements ProtocolCodec{

    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
        int remaining = in.remaining();
        int packlen   = 2;//所需包最小长度
        
        if(remaining < packlen){
            return false;
        }
        in.mark();
        byte b = in.get();
        
        WsRequest request = new WsRequest(
                (b & 0x80) > 0,   //fin
                (b & 0x70) >>> 4,  // rsv
                (byte) (b & 0x0F) // opCode
                );
        b = in.get();//第一位是mask, 后面是playloadlength
        request.hasMask = (b & 0xFF) >> 7 == 1;
        int payloadLength = b & 0x7F;//后面7位
        
        if(payloadLength == 126){//为126读2个字节，后两个字节为payloadLength
            packlen += 2;
            if(remaining < packlen){
                in.reset();
               return false;
            }
            payloadLength = (in.get() & 0xff) << 8;
            payloadLength |= (in.get() & 0xff);
            
        }else if(payloadLength == 127){
            packlen += 8;
            if(remaining < packlen){//127读8个字节,后8个字节为payloadLength
                in.reset();
                return false;
            }
            
            payloadLength = (int) in.getLong();
            
        }
        packlen += payloadLength;
        
        if(remaining < packlen){
            in.reset();
            return false;
        }
        if(request.hasMask){
            byte[] mask = new byte[4];
            in.get(mask);
            request.mask = mask;
        }
        
        if(payloadLength <= 0){
            out.write(request);
            return true;
        }
        
        byte[] t = new byte[payloadLength];
        in.get(t);
        request.body = t;
        out.write(request);
        return true;
    }

    @Override
    public ByteBuffer encode(Session session, Object msg) throws Exception {
        if(msg instanceof ByteBuffer){
            return (ByteBuffer) msg;
        }
        WsResponse response = (WsResponse) msg;
        byte[] body = response.body;
        int bodyLen = 0;
        
        if(body != null){
            bodyLen += body.length;
        }

        ByteBuffer buff = null;
        byte op = (byte) (0x8f & (response.op | 0xf0));
        if(bodyLen < 126){
            buff = session.getWriteCacheBuffer(bodyLen + 2);
            buff.put(op);
            buff.put((byte) bodyLen);
            
        }else if(bodyLen < 65535){
            buff = session.getWriteCacheBuffer(bodyLen + 4);
            buff.put(op);
            buff.put((byte) 126);
            buff.put((byte) (bodyLen >>> 8));
            buff.put((byte) (bodyLen & 0xff));
            
        }else{
            buff = session.getWriteCacheBuffer(bodyLen + 10);
            buff.put(op);
            buff.put((byte) 127);
            buff.putLong(bodyLen);
        }
        
        if(body != null && body.length > 0){
            buff.put(body);
        }

        buff.flip();
        return buff;
    }

}
