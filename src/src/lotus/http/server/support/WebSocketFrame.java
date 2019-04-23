package lotus.http.server.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebSocketFrame {

    public static final byte            OPCODE_TEXT         =   1;
    public static final byte            OPCODE_BINARY       =   2;
    public static final byte            OPCODE_CLOSE        =   8;
    public static final byte            OPCODE_PING         =   9;
    public static final byte            OPCODE_PONG         =   10;
    
    public boolean  fin      =   true;          // 1 bit in length
    public boolean  rsv1     =   false;         // 1 bit in length
    public boolean  rsv2     =   false;         // 1 bit in length
    public boolean  rsv3     =   false;         // 1 bit in length
    public byte     opcode   =   OPCODE_TEXT;   // 4 bits in length
    public boolean  masked   =   false;         // 1 bit in length
    public int      payload  =   0;             // either 7, 7+16, or 7+64 bits in length
    public byte[]   mask     =   null;          // 32 bits in length 可选, 如masked = 0 则此处为空
    public byte[]   body     =   null;          // n*8 bits in length, where n >= 0
    
    
    public void wrap(OutputStream out) throws IOException {
        long datalen = (body != null ? body.length : 0);
        
        byte b1 = 
                (byte)( (fin ? 0x80 : 0x00)  | 
                        (rsv1 ? 0x40 : 0x00) |
                        (rsv2 ? 0x20 : 0x00) |
                        (rsv3 ? 0x10 : 0x00)
                       );
        b1 = (byte) (b1 | (0x0f & opcode));
        
        out.write(b1);
        
        byte b2 = (byte) (masked ? 0x80 : 0x00);
        byte[] len = null;
        if(datalen < 126) {
            b2 = (byte) (b2 | datalen);
            out.write(b2);
        }else if(datalen < 65535) {
            b2 = (byte) (b2 | 126);
            //发送2b长度
            len = new byte[2];
            len[0] = (byte) (datalen >>> 8);
            len[1] = (byte) (datalen & 0xff);
            out.write(b2);
        }else {
            b2 = (byte) (b2 | 127);
            //发送8b长度
            len = new byte[8];
            len[0] = (byte) (datalen & 0xff);
            len[1] = (byte) ((datalen >>> 8) & 0xff);
            len[2] = (byte) ((datalen >>> 16) & 0xff);
            len[3] = (byte) ((datalen >>> 24) & 0xff);
            len[4] = (byte) ((datalen >>> 32) & 0xff);
            len[5] = (byte) ((datalen >>> 40) & 0xff);
            len[6] = (byte) ((datalen >>> 48) & 0xff);
            len[7] = (byte) ((datalen >>> 56) & 0xff);
        }
        
        if(datalen > 0) {
            if(masked) {
                int pLen = body.length;
                for(int i = 0; i < pLen; i++){
                    body[i] = (byte) (body[i] ^ mask[i % 4]);
                }
            }
            
            
        }
        
    }
    
    
    public void unWrap(InputStream in) {
        
    }
    
    
}
