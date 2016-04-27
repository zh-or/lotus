package lotus.socket.common;

import java.nio.ByteBuffer;

import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;
import lotus.util.Util;

public class LengthProtocolCode implements ProtocolCodec{

	@Override
	public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		int total = in.remaining();
		if(total > 3){
			in.mark();
			if(in.get() != 0x02){
				session.closeNow();
				return false;
			}
			byte[] blen = new byte[2];/*len*/
			in.get(blen);
			int packlen = Util.byte2short(blen);
			if(packlen > 65535){
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
				    session.closeNow();
				}
			}
			in.reset();
		}
		return false;
	}

	@Override
	public ByteBuffer encode(Session session, Object msg) throws Exception{
		
		return (ByteBuffer) msg;
	}

}
