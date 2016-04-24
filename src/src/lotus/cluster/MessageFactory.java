package lotus.cluster;

import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;


public class MessageFactory {
    private static MessageFactory   instance    =   null;
    private static Object           lock        =   new Object();

    private static int              listsize    =   1000;
    private static final byte[]		EMPTY_DATA	=	new byte[]{};
    private LinkedBlockingQueue<Message> mq		=	null;
   
    private MessageFactory(){
    	mq = new LinkedBlockingQueue<Message>(listsize);
    }
    
    public static MessageFactory getInstance(){
        if(instance == null){
            synchronized (lock) {
                if(instance == null){
                    instance = new MessageFactory();
                }
            }
        }
        return instance;
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
        
        return getInstance().create(
					        		(type & -128)  == -128,
					        		(byte) (type & 127),
					        		new String(to, charset),
					        		new String(from, charset),
					        		new String(msgid, charset),
					        		new String(head, charset),
					        		body
					        		);
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
    
    public Message create(boolean needReceipt, byte type, String to, String from, String msgid, String head, byte[] body){
        Message msg = mq.poll();
        if(msg == null){
        	msg = new Message(needReceipt, type, to, from, msgid, head, body);
        }else{
        	msg.needReceipt = needReceipt;
        	msg.type = type;
        	msg.to = to;
        	msg.from = from;
        	msg.msgid = msgid;
        	msg.head = head;
        	msg.body = body;
        }
        return msg;
    }
    
    public void destory(Message msg){
    	if(mq.size() < listsize){
    		mq.add(msg);
    	}
    }    
}
