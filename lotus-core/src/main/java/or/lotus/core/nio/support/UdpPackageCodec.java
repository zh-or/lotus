package or.lotus.core.nio.support;

import or.lotus.core.nio.*;

public class UdpPackageCodec implements ProtocolCodec {
    @Override
    public boolean decode(Session session, LotusByteBuf in, ProtocolDecoderOutput out) throws Exception {
        int len = in.getDataLength();
        byte[] data = new byte[len];
        in.get(data);
        out.write(data);
        return true;
    }

    @Override
    public boolean encode(Session session, Object msg, EncodeOutByteBuffer out) throws Exception {
        out.append((byte[]) msg);
        return true;
    }
}
