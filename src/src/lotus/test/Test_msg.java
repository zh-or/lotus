package lotus.test;

import java.nio.charset.Charset;

import lotus.cluster.Message;
import lotus.cluster.MessageFactory;
import lotus.util.Util;

public class Test_msg {
	public static void main(String[] args) {
		MessageFactory mf = MessageFactory.getInstance();
		Message msg = mf.create(true, Message.MTYPE_BROADCAT, "123", "123", "123", "1234", new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
		System.out.println(msg.toString());
		
		byte[] data = MessageFactory.encode(msg, Charset.forName("gbk"));
		System.out.println(Util.byte2str(data));
		
		Message msg2 = MessageFactory.decode(data, Charset.forName("gbk"));
		System.out.println(msg2.toString());
		
	}
}
