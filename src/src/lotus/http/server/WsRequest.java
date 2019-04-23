package lotus.http.server;

/**
 * 简单版 需要修改
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
    public boolean hasMask;
    public byte[]  mask;
    public byte[]  body;
    
    public String  basePath;
    public String  queryString;
    
    private boolean decode = false;

    public WsRequest(boolean fin, int rsv, byte op) {
        this.fin = fin;
        this.rsv = rsv;
        this.op = op;
    }
    
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
    
    
}
