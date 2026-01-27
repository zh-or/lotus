package or.lotus.core.nio.http;

import or.lotus.core.http.WebSocketFrame;
import or.lotus.core.nio.*;

public class WebSocketProtocolCodec  implements ProtocolCodec {

    HttpServer context;

    public WebSocketProtocolCodec(HttpServer context) {
        this.context = context;
    }

    @Override
    public boolean decode(Session session, LotusByteBuf in, ProtocolDecoderOutput out) throws Exception {
        int  remaining = in.getDataLength();
        long packlen   = 0;//所需包最小长度
        int  headLen   = 2;
        if(remaining < headLen) {
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
            if(remaining < headLen) {
                in.reset();
                return false;
            }
            byte[] bytes = new byte[2];
            in.get(bytes);
            //packlen = new BigInteger(bytes).intValue();

            packlen = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
        } else if(frame.payload == 127) {
            headLen += 8;
            if(remaining < headLen){//127读8个字节,后8个字节为payloadLength
                in.reset();
                return false;
            }
            byte[] bytes = new byte[8];
            in.get(bytes);
            //packlen = new BigInteger(bytes).longValue();
            packlen = 0;
            for (int i = 0; i < 8; i++) {
                packlen = (packlen << 8) | (bytes[i] & 0xFF);
            }

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
    public boolean encode(Session session, Object msg, EncodeOutByteBuffer out) throws Exception {

        if(msg instanceof WebSocketFrame) {
            WebSocketFrame frame   = (WebSocketFrame) msg;
            long           dataLen = (frame.body != null ? frame.body.length : 0);

            byte b1 =
                    (byte)( (frame.fin  ? 0x80 : 0x00) |
                            (frame.rsv1 ? 0x40 : 0x00) |
                            (frame.rsv2 ? 0x20 : 0x00) |
                            (frame.rsv3 ? 0x10 : 0x00)
                    );
            b1 = (byte) (b1 | (0x0f & frame.opcode));
            byte b2 = (byte) (frame.masked ? 0x80 : 0x00);

            if(dataLen < 126) {
                b2 = (byte) (b2 | dataLen);
                out.append(b1);
                out.append(b2);
            }else if(dataLen <= 65535) {
                b2 = (byte) (b2 | 126);
                //发送2b长度
                out.append(b1);
                out.append(b2);
                out.append((byte) (dataLen >>> 8));
                out.append((byte) (dataLen & 0xff));
            }else {
                b2 = (byte) (b2 | 127);
                //发送8b长度
                out.append(b1);
                out.append(b2);
                out.append((byte) ((dataLen >>> 56) & 0xff));
                out.append((byte) ((dataLen >>> 48) & 0xff));
                out.append((byte) ((dataLen >>> 40) & 0xff));
                out.append((byte) ((dataLen >>> 32) & 0xff));
                out.append((byte) ((dataLen >>> 24) & 0xff));
                out.append((byte) ((dataLen >>> 16) & 0xff));
                out.append((byte) ((dataLen >>> 8) & 0xff));
                out.append((byte) (dataLen & 0xff));
            }
            if(frame.mask != null && frame.masked) {
                out.append(frame.mask);
            }
            if(dataLen > 0) {
                out.append(frame.body);
            }

        } else if(msg instanceof HttpResponse) {
            //升级协议时需要返回该头
            HttpResponse response = (HttpResponse) msg;
            out.append(response.getHeaderBytes());
        }

        return true;
    }
}
