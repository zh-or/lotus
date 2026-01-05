package or.lotus.core.nio.support;

import or.lotus.core.common.Utils;
import or.lotus.core.nio.*;


/**
 * 使用此解码器需要注意的是, 包长度使用的是2字节数据 即最大一个包不能超过 65535 字节
 *
 * @author or
 */
public class LengthProtocolCode implements ProtocolCodec {

    @Override
    public boolean decode(Session session, LotusByteBuf in, ProtocolDecoderOutput out) throws Exception {
        int total = in.getDataLength();
        if (total > 3) {
            in.mark();
            if (in.get() != 0x02) {
                session.closeNow();
                return false;
            }
            byte[] hLen = new byte[2];/*len*/
            in.get(hLen);
            int packLen = Utils.byte2short(hLen);
            if (packLen > 65535) {
                session.closeNow();
                return false;
            }
            if (packLen > 0 && packLen <= total) {
                byte[] packData = new byte[packLen - 4];
                in.get(packData);
                if (in.get() == 0x03) {
                    out.write(packData);
                } else {
                    session.closeNow();
                }
                return true;
            }
            in.reset();
        }
        return false;
    }

    @Override
    public boolean encode(Session session, Object msg, EncodeOutByteBuffer out) throws Exception {

        byte[] content = (byte[]) msg;
        int len = content.length + 2 + 2;
        out.append((byte) 0x02);
        out.append(Utils.short2byte(len));
        out.append(content);
        out.append((byte) 0x03);
        return true;
    }

}
