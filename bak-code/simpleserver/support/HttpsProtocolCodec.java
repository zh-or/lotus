package or.lotus.core.http.simpleserver.support;

import java.nio.ByteBuffer;

import or.lotus.core.http.simpleserver.HttpServer;
import or.lotus.core.nio.LotusIOBuffer;
import or.lotus.core.nio.ProtocolCodec;
import or.lotus.core.nio.ProtocolDecoderOutput;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.tcp.NioTcpSession;

public class HttpsProtocolCodec implements ProtocolCodec {
    // 19 -- 25
    // private final static byte HTTPS_REQ_START = 0x16;

    private HttpServer context = null;
    private HttpProtocolCodec httpProtocolCodec = null;

    public HttpsProtocolCodec(HttpServer context) {
        this.context = context;
        httpProtocolCodec = context.getHttpProtocolCodec();
    }

    /***
     * 处理握手
     *
     * @param session
     * @return 如果握手完成后有多余的数据则返回, 会流转到decode处理
     * @throws Exception
     *             触发异常则会关闭此连接
     */
    public ByteBuffer doHandshake(NioTcpSession session) throws Exception {
        /*
         * https://www.cnblogs.com/LittleHann/p/3733469.html?utm_source=tuicool&
         * utm_medium=referral tls/ssl 协议起始字节 1) CHANGE_CIPHER_SPEC 20 0x14 2)
         * ALERT 21 0x15 3) HANDSHAKE 22 0x16 4) APPLICATION_DATA 23 0x17
         */
        ByteBuffer tmpBuf = session.getWriteCacheBuffer(0);
        int n = session.readFromChannel(tmpBuf);
        if (n < 0) {
            // 已关闭
            return null;
        }
        tmpBuf.flip();
        tmpBuf.mark();
        byte begin = tmpBuf.get();
        if (begin > 19 && begin < 25 && context.isEnableSSL()) {// 是否https
            tmpBuf.reset();
            SSLState state = new SSLState(context, session);
            session.setAttr(SSLState.SSL_STATE_KEY, state);
            // 握手可能有剩余app数据
            ByteBuffer handshakeRes = state.doHandshake(tmpBuf);

            return handshakeRes;
        }
        tmpBuf.reset();
        return tmpBuf;
    }

    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {

        SSLState state = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
        ByteBuffer outBuffer = null;
        if (state != null) {
            outBuffer = session.getWriteCacheBuffer(state.getAppBufferSize());
            if (!state.unwrap(in, outBuffer)) {
                session.putWriteCacheBuffer(outBuffer);
                return false;
            }
            outBuffer.flip();
        } else {
            // 使用的http协议
            outBuffer = in;
        }
        return httpProtocolCodec.decode(session, outBuffer, out);

    }

    @Override
    public boolean encode(Session session, Object msg, LotusIOBuffer out) throws Exception {

        if (context.isEnableSSL()) {
            // 启用ssl时并且使用ssl协议访问
            SSLState state = (SSLState) session.getAttr(SSLState.SSL_STATE_KEY);
            if (state != null) {

                LotusIOBuffer tmpOut = new LotusIOBuffer(session.getContext());
                boolean r = httpProtocolCodec.encode(session, msg, tmpOut);
                ByteBuffer[] bufs = tmpOut.getAllBuffer();
                ByteBuffer outBuf;
                for (ByteBuffer buf : bufs) {
                    buf.flip();
                    do {
                        outBuf = session.getWriteCacheBuffer(state.getNetBufferSize());
                        state.wrap(buf, outBuf);
                        out.append(outBuf);
                    } while(buf.hasRemaining());
                }
                tmpOut.free();
                return r;
            }

        }
        return httpProtocolCodec.encode(session, msg, out);
    }

}
