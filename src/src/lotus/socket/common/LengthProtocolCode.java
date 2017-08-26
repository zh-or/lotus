package lotus.socket.common;

import java.nio.ByteBuffer;

import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;
import lotus.util.Util;

/**
 * 使用此解码器需要注意的是, 包长度使用的是2字节数据 即最大一个包不能超过 65535 字节
 * @author or
 *
 */
public class LengthProtocolCode implements ProtocolCodec{
	@Override
	public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		int total = in.remaining();
		if(total > 3){
			in.mark();
			if(in.get() != 0x02){
			    in.reset();
			    byte[] tmp = new byte[in.remaining()];
			    in.get(tmp);
			    System.out.println("包头不对, 总长度:" + tmp.length + ", data:" + Util.byte2str(tmp));
				session.closeNow();
				return false;
			}
			byte[] blen = new byte[2];/*len*/
			in.get(blen);
			int packlen = Util.byte2short(blen);
			if(packlen > 65535){
			    System.out.println("包长度过长");
				session.closeNow();
				return false;
			}
			if(packlen > 0 && packlen <= total){
				byte[] bpack = new byte[packlen - 4];
				in.get(bpack);
				if(in.get() == 0x03){
					out.write(bpack);
	                return true;
				}else{
				    System.out.println("包尾不对");
				    session.closeNow();
				    return true;
				}
			}
	        in.reset();
		}
		return false;
	}

	@Override
	public ByteBuffer encode(Session session, Object msg) throws Exception{
	    
	    byte[] content = (byte[]) msg;
	    
        byte[] send =  new byte[content.length + 2 + 2];
        send[0] = 0x02;
        send[send.length - 1] = 0x03;
        byte[] len = Util.short2byte(send.length);

        send[1] = len[0];
        send[2] = len[1];

        System.arraycopy(content, 0, send, 3, content.length);
        ByteBuffer buff = ByteBuffer.allocate(send.length);
        buff.put(send);
        buff.flip();
        return buff;
	}

}
