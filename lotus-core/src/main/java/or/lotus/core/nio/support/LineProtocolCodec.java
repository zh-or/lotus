package or.lotus.core.nio.support;

import or.lotus.core.nio.*;

/**
 * 示例解码器 一行一个包
 * @author OR
 */
public class LineProtocolCodec implements ProtocolCodec {
	private byte[] line;

	public LineProtocolCodec() {
	    this("\n");
	}

	public LineProtocolCodec(String lineChar) {
		this.line = lineChar.getBytes();
	}

	@Override
	public boolean decode(Session session, LotusByteBuf in, ProtocolDecoderOutput out) throws Exception {
	    /*
	     * 这里不能用 in.array() 来读取数据, 因为这个ByteBuffer 是使用 ByteBuffer.allocateDirect 分配的.
	     * in.array() 将会抛出异常
	     * */

		int p = in.search(line);
		if(p != -1) {
			byte[] arr = new byte[p + 1];
			in.get(arr);
			out.write(arr);
			return true;
		}

		return false;
	}

	@Override
	public boolean encode(Session session, Object msg, EncodeOutByteBuffer out) throws Exception {
	    byte[] content = (byte[]) msg;
        out.append(content);
        out.append(line);
        return true;
    }

}
