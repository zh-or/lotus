package lotus.http.server.support;

import java.io.UnsupportedEncodingException;

import lotus.utils.Utils;

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
    
    private boolean decodeMask =   false;//已解码
    
    public WebSocketFrame(byte op) {
        opcode = op;
    }

    public static WebSocketFrame text(String str) throws UnsupportedEncodingException {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_TEXT);
        frame.body = str.getBytes("utf-8");
        return frame;
    }


    public static WebSocketFrame binary(byte[] bin) {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_BINARY);
        frame.body = bin;
        return frame;
    }

    public static WebSocketFrame ping() {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_PING);
        return frame;
    }

    public static WebSocketFrame pong() {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_PONG);
        return frame;
    }
    
    public static WebSocketFrame close() {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_CLOSE);
        return frame;
    }
    
    public String getText() {
        return new String(getBinary());
    }
    
    public byte[] getBinary() {
        if(masked) {
            if(!decodeMask) {
                int len = body.length;
                for(int i = 0; i < len; i++){
                    body[i] = (byte) (body[i] ^ mask[i % 4]);
                }
                decodeMask = true;
            }
        }
        return body;
    }
    
    /**
     * 使用mask
     * @return
     */
    public WebSocketFrame mask() {
        byte[] tMask = new byte[4];
        tMask[0] = (byte) Utils.RandomNum(0, 255);
        tMask[1] = (byte) Utils.RandomNum(0, 255);
        tMask[2] = (byte) Utils.RandomNum(0, 255);
        tMask[3] = (byte) Utils.RandomNum(0, 255);
        return mask(tMask);
    }

    /**
     * 使用mask
     * @return
     */
    public WebSocketFrame mask(byte[] mask) {
        this.mask = mask;
        this.masked = true;
        return this;
    }
    
}
