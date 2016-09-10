package lotus.cluster;

import java.util.Arrays;

public class NetPack {
    /* type data*/
    public static final byte CMD_INIT           =       0x01;//上线
    public static final byte CMD_MSG            =       0x02;//消息
    public static final byte CMD_RES            =       0x03;//回执
    public static final byte CMD_KEEP           =       0x04;//心跳
    public static final byte CMD_SUBS_MSG       =       0x05;//订阅某类型消息
    public static final byte CMD_UNSUBS_MSG     =       0x06;//取消订阅
    
    public byte type        =   0x00;
    public byte[] body      =   null;
    
    /**
     * data.length 必须大于0
     * @param data
     */
    public NetPack(byte[] data){
        this.type = data[0];
        if(data.length > 1){
            this.body = new byte[data.length - 1];
            System.arraycopy(data, 1, this.body, 0, body.length);
        }
    }
    
    public NetPack(byte type, byte[] body){
        this.type = type;
        this.body = body;
    }
    
    public byte[] Encode(){
        int len  = body != null ? body.length : 0;
        byte[] data = new byte[len + 1];
        data[0] = type;
        if(len > 0){
            System.arraycopy(body, 0, data, 1, len);
        }
        return data;
    }

    @Override
    public String toString() {
        return "NetPack [type=" + type + ", body=" + Arrays.toString(body) + "]";
    }
}
