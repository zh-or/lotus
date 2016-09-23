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
	    /*
	     * 这里不能用 in.array() 来读取数据, 因为这个ByteBuffer 是使用 ByteBuffer.allocateDirect 分配的.
	     * in.array() 将会抛出异常
	     * */
	    in.mark();
        int size = in.limit();
        byte[] arr = new byte[size];
        in.get(arr);
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
	    ByteBuffer out = session.getWriteCacheBuffer(data.length);
	    out.put(data);
	    out.flip();
	    return out;
	}

}
