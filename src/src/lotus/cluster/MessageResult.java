package lotus.cluster;

import java.nio.charset.Charset;

/**
 * tcp 本身是不丢包
 * @author or
 */
public class MessageResult {
	public boolean isrecv;
	public String msgid;
	public String to;
	
	public MessageResult(boolean isrecv, String msgid, String to){
		this.isrecv = isrecv;
		this.msgid = msgid;
		this.to = to;
	}
	
	/**
	 * 外部保证此data不为 null
	 * @param data
	 * @throws Exception 
	 */
	public MessageResult(byte[] data, Charset charset) throws Exception{
	    if(data == null || data.length < 2) throw new Exception("data error");
		int len = data[1];
		isrecv = data[0] > 0;
		byte[] id = new byte[len];
		System.arraycopy(data, 2, id, 0, len);
		msgid = new String(id, charset);
		len = data[len + 2];
		byte[] _to = new byte[len];
		System.arraycopy(data, data.length - len, _to, 0, len);
		to = new String(_to, charset);
	}
	
	public byte[] Encode(Charset charset){
	    int len_id = 0, len_to = 0;
		byte[] id = msgid.getBytes(charset);
		byte[] _to = to.getBytes(charset);
		len_id = id.length;
		len_to = _to.length;
		byte[] data = new byte[len_id + len_to + 3];
		data[0] = (byte) (isrecv ? 0x01 : 0x00);
		data[1] = (byte) len_id;
        data[len_id + 2] = (byte) len_to;
		System.arraycopy(id, 0, data, 2, len_id);
		System.arraycopy(_to, 0, data, len_id + 3, len_to);
		return data;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MessageResult [isrecv=");
        builder.append(isrecv);
        builder.append(", msgid=");
        builder.append(msgid);
        builder.append(", to=");
        builder.append(to);
        builder.append("]");
        return builder.toString();
    }
	
	
	
	
}
