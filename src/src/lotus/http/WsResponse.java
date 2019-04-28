package lotus.http;

import java.util.Arrays;

public class WsResponse {

    public boolean fin;
    public int     rsv;
    public byte    op;
    public boolean hasMask;
    public byte[]  mask;
    public byte[]  body;

    private boolean decode = false;
    
    public WsResponse(boolean fin, int rsv, byte op) {
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

    @Override
    public String toString() {
        return "WsResponse [fin=" + fin + ", rsv=" + rsv + ", op=" + op + ", hasMask=" + hasMask + ", mask=" + Arrays.toString(mask) + ", body=" + Arrays.toString(body) + "]";
    }
    
    
}
