package or.lotus.core.nio.support;

import or.lotus.core.nio.LotusByteBuffer;
import or.lotus.core.nio.support.NioSession;

import java.nio.ByteBuffer;

public interface ProtocolCodec {
    /**
     * 接收数据解码器
     * @param session
     * @param in
     * @param out
     * @return 返回true时表示数据接收完毕, 返回false表示数据还未接收完毕, 返回true时如果缓存的buffer已经装满会自动扩容
     * @throws Exception
     */
    public boolean decode(NioSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception;

    /**
     * 发送数据编码器
     * @param session
     * @param msg
     * @param out
     * @return 返回 false 表示数据还未发送完毕, 返回 true 表示数据已发送完毕, 当返回 false 时IO线程会一直循环调用此方法会阻塞IO线程
     * @throws Exception
     */
    public boolean encode(NioSession session, Object msg, LotusByteBuffer out) throws Exception;
}
