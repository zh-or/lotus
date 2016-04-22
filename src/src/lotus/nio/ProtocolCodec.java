package lotus.nio;

import java.nio.ByteBuffer;

public interface ProtocolCodec {
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out);
    public ByteBuffer encode(Session session, Object msg);
}
