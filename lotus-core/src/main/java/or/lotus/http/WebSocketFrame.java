package or.lotus.http;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import or.lotus.db.Utils;

public class WebSocketFrame {

    public static final byte            OPCODE_CONTINUATION =   0;
    public static final byte            OPCODE_TEXT         =   1;
    public static final byte            OPCODE_BINARY       =   2;
    public static final byte            OPCODE_CLOSE        =   8;
    public static final byte            OPCODE_PING         =   9;
    public static final byte            OPCODE_PONG         =   10;
/*    1. FIN=0，opcode=0x1，表示发送的是文本类型，且消息还没发送完成，还有后续的数据帧。
      2. FIN=0，opcode=0x0，表示消息还没发送完成，还有后续的数据帧，当前的数据帧需要接在上一条数据帧之后。
      3. FIN=1，opcode=0x0，表示消息已经发送完成，没有后续的数据帧，当前的数据帧需要接在上一条数据帧之后。服务端可以将关联的数据帧组装成完整的消息
*/
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

    /* https://tools.ietf.org/html/rfc6455  */

    public WebSocketFrame(byte op) {
        opcode = op;
    }

    public static WebSocketFrame text(String str)  {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_TEXT);
        try {
            frame.body = str.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
        return pong(null);
    }

    public static WebSocketFrame pong(byte[] data) {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_PONG);
        frame.body = data;
        return frame;
    }

    public static WebSocketFrame close() {
        WebSocketFrame frame = new WebSocketFrame(OPCODE_CLOSE);
        return frame;
    }

    public String getText() throws Exception {
        return new String(getBinary(), "utf-8");
    }

    public byte[] getBinary() {
        if(masked && body != null) {
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
     * 某些服务器必须加mask才能通讯
     * @return
     */
    public WebSocketFrame mask() {
        int rand = Utils.RandomNum(0, Integer.MAX_VALUE);
        byte[] tMask = new byte[4];
        tMask[0] = (byte) (rand & 0xff000000 >>> 24);
        tMask[1] = (byte) (rand & 0x00ff0000 >>> 16);
        tMask[2] = (byte) (rand & 0x0000ff00 >>> 8);
        tMask[3] = (byte) (rand & 0x000000ff );

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

    @Override
    public String toString() {
        return "WebSocketFrame [fin=" + fin + ", rsv1=" + rsv1 + ", rsv2=" + rsv2 + ", rsv3=" + rsv3 + ", opcode=" + opcode + ", masked=" + masked + ", payload=" + payload + ", mask=" + Arrays.toString(mask) + ", body=" + (body == null ? "null" : body.length) + "]" + (opcode == OPCODE_TEXT ? ("\n body=" + new String(getBinary())) : "");
    }

}
