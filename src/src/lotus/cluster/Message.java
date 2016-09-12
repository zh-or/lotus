package lotus.cluster;

import java.nio.charset.Charset;
import java.util.Arrays;

public class Message {
    /**
     * 普通消息
     */
    public static final byte MTYPE_MESSAGE           =   0x01;
    /**
     * 广播消息, 发送给所有节点
     */
    public static final byte MTYPE_BROADCAT          =   0x02;
    /**
     * 订阅消息, 发送给所有订阅此消息的节点
     */
    public static final byte MTYPE_SUBSCRIBE         =   0x03;

    private static final byte[]     EMPTY_DATA  =   new byte[]{};
    
    
    public boolean      needReceipt = false;
    public byte         type;
    public String       to;
    public String       from;
    public String       msgid;
    public String       head;
    public byte[]       body;
    
	public Message(byte type, String to, String msgid, String head, byte[] body) {
		this.type = type;
		this.to = to;
		this.msgid = msgid;
		this.head = head;
		this.body = body;
	}

	public Message IsNeedReceipt(boolean needReceipt){
	    this.needReceipt = needReceipt;
	    return this;
	}
	
	
	@Override
	public String toString() {
		return "Message [needReceipt=" + needReceipt + ", type=" + type + ", to=" + to + ", from=" + from + ", msgid=" + msgid
				+ ", head=" + head + ", body=" + Arrays.toString(body) + "]";
	}
	
    /**
     * 从byte创建Message
     * @param data
     * @param charset
     * @return
     */
    public static Message decode(byte[] data, Charset charset){
        if(data == null || data.length < 5){
            return null;/*不能识别的数据包*/
        }
        int bound = 0;
        byte type = data[bound];
        bound ++;
        byte[] to = destr(data, bound);
        bound += to.length + 1;
        byte[] from = destr(data, bound);
        bound += from.length + 1;
        byte[] msgid = destr(data, bound);
        bound += msgid.length + 1;
        byte[] head = destr(data, bound);
        bound += head.length + 1;
        int len_body = data.length - bound;
        byte[] body = new byte[len_body];
        System.arraycopy(data, bound, body, 0, len_body);
        Message msg = new Message(
                                    (byte) (type & 127),
                                    new String(to, charset),
                                    new String(msgid, charset),
                                    new String(head, charset),
                                    body
                                    );
        msg.IsNeedReceipt((type & -128)  == -128);
        msg.from = new String(from, charset);
        return msg;
    }
    
    private static byte[] destr(byte[] data, int offset){
        int len = data[offset];
        if(len > 0){
            byte[] strdata = new byte[len];
            System.arraycopy(data, offset + 1, strdata, 0, len);
            return strdata;
        }
        return EMPTY_DATA;
    }
    
    /**
     * 编码一条消息
     * @param msg
     * @param charset
     * @return 如果返回空则表示此条消息长度过大
     */
    public static byte[] encode(Message msg, Charset charset){
        int count = 1;
        int bound =  0;
        int len_to, len_from, len_msgid, len_head;
        byte[] to = msg.to.getBytes(charset);
        byte[] from = msg.from.getBytes(charset);
        byte[] msgid = msg.msgid.getBytes(charset);
        byte[] head = msg.head.getBytes(charset);
        
        len_to = (to.length > 255 ? 255 : to.length);
        len_from = (from.length > 255 ? 255 : from.length);
        len_msgid = (msgid.length > 255 ? 255 : msgid.length);
        len_head = (head.length > 255 ? 255 : head.length);
        count += len_to + 1;
        count += len_from + 1;
        count += len_msgid + 1;
        count += len_head + 1;
        count += msg.body == null ? 0 : msg.body.length;
        
        if(count > 65535){
            return null;
        }
        byte[] msgdata = new byte[count];
        
        msgdata[bound] = (byte)(msg.needReceipt ? msg.type | -128 : msg.type);/*type 最高位 为1 表示此消息需要回执*/
        bound++;
        
        msgdata[bound] = (byte) len_to;
        bound ++;
        System.arraycopy(to, 0, msgdata, bound, len_to);
        bound+= len_to;
        
        msgdata[bound] = (byte) len_from;
        bound++;
        System.arraycopy(from, 0, msgdata, bound, len_from);
        bound += len_from;
        
        msgdata[bound] = (byte) len_msgid;
        bound ++;
        System.arraycopy(msgid, 0, msgdata, bound, len_msgid);
        bound += len_msgid;
        
        msgdata[bound] = (byte) len_head;
        bound ++;
        System.arraycopy(head, 0, msgdata, bound, len_head);
        bound += len_head;
        
        if(msg.body != null){
            System.arraycopy(msg.body, 0, msgdata, bound, msg.body.length);
        }
        
        return msgdata;
    }
}
