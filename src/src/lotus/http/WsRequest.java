package lotus.http;

import java.nio.charset.Charset;

import lotus.utils.Utils;

/**
 * 1. FIN=0，opcode=0x1，表示发送的是文本类型，且消息还没发送完成，还有后续的数据帧。
 * 2. FIN=0，opcode=0x0，表示消息还没发送完成，还有后续的数据帧，当前的数据帧需要接在上一条数据帧之后。
 * 3. FIN=1，opcode=0x0，表示消息已经发送完成，没有后续的数据帧，当前的数据帧需要接在上一条数据帧之后。服务端可以将关联的数据帧组装成完整的消息。
 * @author or
 *
 */
public class WsRequest {
    
    public boolean fin;
    public int     rsv;
    public byte    op;
    public boolean hasMask = false;
    public byte[]  mask;
    public byte[]  body;
    
    
    private boolean decode = false;

    
    public byte[] getBody(){
        if(body == null){
            return null;
        }
        if(!decode && hasMask){
            decode = true;
            for(int i = 0; i < body.length; i++){
                body[i] = (byte) (body[i] ^ mask[i % 4]);
            }
        }
        
        return body;
    }
    
    public static WsRequest ping(){
        WsRequest req = new WsRequest();
        req.op = WsStatus.OPCODE_PING;
        return req;
    }
    

    public static WsRequest close(){
        WsRequest req = new WsRequest();
        req.op = WsStatus.OPCODE_CLOSE;
        return req;
    }
    
    
    /**
     * 默认为utf-8编码
     * @param str
     * @return
     */
    public static WsRequest text(String str){
        return text(str, Charset.forName("utf-8"));
    }
    
    public static WsRequest text(String str, Charset charset){
        WsRequest req = new WsRequest();
        req.op = WsStatus.OPCODE_TEXT;
        req.body = str.getBytes(charset);
        return req;
    }

    public WsRequest mask() {
        byte[] tMask = new byte[4];
        tMask[0] = (byte) Utils.RandomNum(-128, 127);
        tMask[1] = (byte) Utils.RandomNum(-128, 127);
        tMask[2] = (byte) Utils.RandomNum(-128, 127);
        tMask[3] = (byte) Utils.RandomNum(-128, 127);
        return mask(tMask);
    }
    public WsRequest mask(byte[] mask) {
        this.mask = mask;
        return this;
    }
    
    public static WsRequest binary(byte[] bin){
        WsRequest req = new WsRequest();
        req.op = WsStatus.OPCODE_BINARY;
        req.body = bin;
        return req;
    }
}
