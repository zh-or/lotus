package lotus.http.server.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FrameStream {

    public static void wrap(WebSocketFrame frame, OutputStream out) throws IOException {
        long datalen = (frame.body != null ? frame.body.length : 0);
        
        byte b1 = 
                (byte)( (frame.fin ? 0x80 : 0x00)  | 
                        (frame.rsv1 ? 0x40 : 0x00) |
                        (frame.rsv2 ? 0x20 : 0x00) |
                        (frame.rsv3 ? 0x10 : 0x00)
                       );
        b1 = (byte) (b1 | (0x0f & frame.opcode));
        
        out.write(b1);
        
        byte b2 = (byte) (frame.masked ? 0x80 : 0x00);
        if(datalen < 126) {
            b2 = (byte) (b2 | datalen);
            out.write(b2);
        }else if(datalen < 65535) {
            b2 = (byte) (b2 | 126);
            out.write(b2);
            //发送2b长度
            out.write((int) (datalen >>> 8));
            out.write((int) (datalen & 0xff));
        }else {
            b2 = (byte) (b2 | 127);
            out.write(b2);
            //发送8b长度
            out.write((int) (datalen & 0xff));
            out.write((int) ((datalen >>> 8) & 0xff));
            out.write((int) ((datalen >>> 16) & 0xff));
            out.write((int) ((datalen >>> 24) & 0xff));
            out.write((int) ((datalen >>> 32) & 0xff));
            out.write((int) ((datalen >>> 40) & 0xff));
            out.write((int) ((datalen >>> 48) & 0xff));
            out.write((int) ((datalen >>> 56) & 0xff));
        }
        
        if(datalen > 0) {
            if(frame.masked) {
                int pLen = frame.body.length;
                for(int i = 0; i < pLen; i++){
                    frame.body[i] = (byte) (frame.body[i] ^ frame.mask[i % 4]);
                }
            }
            out.write(frame.body);
        }
    }
    
    
    public static WebSocketFrame unWrap(InputStream in) throws IOException {
        int b = in.read();
        WebSocketFrame frame = new WebSocketFrame((byte) (b & 0xf));
        frame.fin = (b & 0x80) != 0;
        frame.rsv1 = (b & 0x40) != 0;
        frame.rsv2 = (b & 0x20) != 0;
        frame.rsv3 = (b & 0x10) != 0;
        
        b = in.read();
        frame.masked = (b & 0x80) != 0;
        frame.payload = b & 0x7f;
        
        long len = 0;
        if(frame.payload == 126) {
            len = in.read() & 0xff << 8;
            len |= (in.read() & 0xff);
        } else if(frame.payload == 127) {

            len = in.read() & 0xff;
            len |= in.read() << 8 & 0xff00l;
            len |= in.read() << 16 & 0xff0000l;
            len |= in.read() << 24 & 0xff000000l;
            len |= in.read() << 32 & 0xff00000000l;
            len |= in.read() << 40 & 0xff0000000000l;
            len |= in.read() << 48 & 0xff000000000000l;
            len |= in.read() << 56 & 0xff00000000000000l;
        } else {
            len = frame.payload;
        }
        
        if(frame.masked) {
            frame.mask = new byte[4];
            in.read(frame.mask);
        }
        
        if(len > 0) {
            /// 127 就读不出来了 ///
            frame.body = new byte[(int) len];
            in.read(frame.body);
        }
        
        return frame;
    }
    
}
