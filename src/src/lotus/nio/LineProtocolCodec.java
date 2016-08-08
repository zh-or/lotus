package lotus.nio;

import java.nio.ByteBuffer;

/**
 * 示例解码器 一行一个包
 * @author OR
 */
public class LineProtocolCodec implements ProtocolCodec{
	private char line			=	'\n';

	public LineProtocolCodec() {
	    this('\n');
	}

	public LineProtocolCodec(char linechar){
		this.line = linechar;
	}
	
	@Override
	public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		in.mark();
		byte[] arr = in.array();
        int size = in.limit();
		for(int i = 0; i < size; i++){
			if(arr[i] == line){
				in.reset();
				byte[] dst = new byte[i + 1];
				
				in.get(dst);
				out.write(dst);
				return true;
			}
		}
		in.reset();
		return false;
	}

	@Override
	public ByteBuffer encode(Session session, Object msg)  throws Exception{
		byte[] data = (byte[]) msg;
	    ByteBuffer out = ByteBuffer.wrap(data);
		return out;
	}

}
