package or.lotus.core.nio.support;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.LotusByteBuffer;

import java.nio.ByteBuffer;


/**
 * 使用此解码器需要注意的是, 包长度使用的是2字节数据 即最大一个包不能超过 65535 字节
 * @author or
 *
 */
public class LengthProtocolCode implements ProtocolCodec{

	@Override
	public boolean decode(NioSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		int total = in.remaining();
		if(total > 3){
			in.mark();
			if(in.get() != 0x02){
			    in.reset();
			    byte[] tmp = new byte[in.remaining()];
			    in.get(tmp);
			    //System.out.println("包头不对, 总长度:" + tmp.length + ", data:" + Utils.byte2str(tmp));
				session.closeNow();
				return false;
			}
			byte[] blen = new byte[2];/*len*/
			in.get(blen);
			int packlen = Utils.byte2short(blen);
			if(packlen > 65535){
			    //System.out.println("包长度过长");
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
				    //System.out.println("包尾不对");
				    session.closeNow();
				    return true;
				}
			}
	        in.reset();
		}
		return false;
	}

	@Override
	public boolean encode(NioSession session, Object msg, LotusByteBuffer out) throws Exception {

        byte[] content = (byte[]) msg;
        int len = content.length + 2 + 2;
        out.append((byte) 0x02);
        out.append(Utils.short2byte(len));
        out.append(content);
        out.append((byte) 0x03);
        return true;
	}

}
