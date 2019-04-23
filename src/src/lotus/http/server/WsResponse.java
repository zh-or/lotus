package lotus.http.server;

import java.nio.charset.Charset;

/**
 * 简单版, 还需要修改
 * @author yf
 *
 */
public class WsResponse {

    public boolean fin;
    public int     rsv;
    public byte    op;
    public boolean hasMask;
    public byte[]  mask;
    public byte[]  body;
    
    
    public static WsResponse pong(){
        WsResponse response = new WsResponse();
        response.op = HttpServer.OPCODE_PONG;
        return response;
    }
    

    public static WsResponse close(){
        WsResponse response = new WsResponse();
        response.op = HttpServer.OPCODE_CLOSE;
        return response;
    }
    
    
    /**
     * 默认为utf-8编码
     * @param str
     * @return
     */
    public static WsResponse text(String str){
        return text(str, Charset.forName("utf-8"));
    }
    
    public static WsResponse text(String str, Charset charset){
        WsResponse response = new WsResponse();
        response.op = HttpServer.OPCODE_TEXT;
        response.body = str.getBytes(charset);
        return response;
    }
    
    public static WsResponse binary(byte[] bin){
        WsResponse response = new WsResponse();
        response.op = HttpServer.OPCODE_BINARY;
        response.body = bin;
        return response;
    }
    
    
}
