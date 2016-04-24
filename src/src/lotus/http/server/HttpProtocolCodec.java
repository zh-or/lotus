package lotus.http.server;

import java.nio.ByteBuffer;

import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;

public class HttpProtocolCodec implements ProtocolCodec{
    
    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) {
        
        return false;
    }

    @Override
    public ByteBuffer encode(Session session, Object msg) {
        
        return null;
    }
}
