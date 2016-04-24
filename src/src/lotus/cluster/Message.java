package lotus.cluster;

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
    
    public boolean      needReceipt;
    public byte         type;
    public String       to;
    public String       from;
    public String       msgid;
    public String       head;
    public byte[]       body;
    
	public Message(boolean needReceipt, byte type, String to, String from, String msgid, String head, byte[] body) {
		this.needReceipt = needReceipt;
		this.type = type;
		this.to = to;
		this.from = from;
		this.msgid = msgid;
		this.head = head;
		this.body = body;
	}

	
	
	@Override
	public String toString() {
		return "Message [needReceipt=" + needReceipt + ", type=" + type + ", to=" + to + ", from=" + from + ", msgid=" + msgid
				+ ", head=" + head + ", body=" + Arrays.toString(body) + "]";
	}
	
	
}
