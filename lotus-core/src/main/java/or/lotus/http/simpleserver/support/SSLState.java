package or.lotus.http.simpleserver.support;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import or.lotus.http.simpleserver.HttpServer;
import or.lotus.nio.tcp.NioTcpSession;

public class SSLState {
    public final static  String     SSL_STATE_KEY          =   "___SSL_STATE_KEY___";
    private static final ByteBuffer emptyBuff              =   ByteBuffer.allocate(0);

    @SuppressWarnings("unused")
    private HttpServer server;
    private SSLEngine       engine;
    private NioTcpSession session;

    private ByteBuffer      appInBuffer;
    private ByteBuffer      netInBuffer;

    private ByteBuffer      netOutBuffer;

    private int             appBufferSize;
    private int             netBufferSize;

    public SSLState(HttpServer server, NioTcpSession session) throws SSLException {
        this.server = server;
        this.session = session;
        SSLContext ssl = server.getSSLContext();
        InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
        engine = ssl.createSSLEngine(address.getHostName(), address.getPort());
        /*
         * See https://github.com/TooTallNate/Java-WebSocket/issues/466
         *
         * We remove TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 from the enabled ciphers since it is just available when you patch your java installation directly.
         * E.g. firefox requests this cipher and this causes some dcs/instable connections
         */
        List<String> ciphers = new ArrayList<>(Arrays.asList(engine.getEnabledCipherSuites()));
        ciphers.remove("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        engine.setEnabledCipherSuites(ciphers.toArray(new String[ciphers.size()]));

        boolean isNeedClientAuth = server.isNeedClientAuth();

        engine.setUseClientMode(false);
        engine.setNeedClientAuth(isNeedClientAuth);
        engine.setWantClientAuth(isNeedClientAuth);
        SSLSession sslSession = engine.getSession();
        appBufferSize = sslSession.getApplicationBufferSize();
        netBufferSize = sslSession.getPacketBufferSize();

        //不知道为什么要加50, 因为jdk文档是这样写的
        server.reSizeCacheBuffer(appBufferSize + 50);

        appInBuffer = session.getWriteCacheBuffer(appBufferSize);
        netInBuffer = session.getWriteCacheBuffer(netBufferSize);
        netOutBuffer = session.getWriteCacheBuffer(netBufferSize);

        appInBuffer.clear();
        netInBuffer.clear();
        netOutBuffer.clear();
        engine.beginHandshake();
    }


    public boolean isHandshaked() {
        HandshakeStatus status = engine.getHandshakeStatus();
        return status == HandshakeStatus.FINISHED || status == HandshakeStatus.NOT_HANDSHAKING;
    }

    /***
     * @return 返回剩余数据
     * @throws Exception
     */
    public ByteBuffer doHandshake(ByteBuffer buf) throws Exception {
        HandshakeStatus status = engine.getHandshakeStatus();
        if(status == HandshakeStatus.NOT_HANDSHAKING) {
            return buf;
        }
        netInBuffer.put(buf);
        netInBuffer.flip();
        session.putWriteCacheBuffer(buf);

        session.writeToChannel(emptyBuff);// initializes res

        do {
            if(engine.isInboundDone() || engine.isOutboundDone()) {
                throw new SSLClosedException();
            }

            switch(status) {
                case NEED_UNWRAP:
                    if(!unwrap(netInBuffer, appInBuffer)) {
                        netInBuffer.compact();
                        session.readFromChannel(netInBuffer);
                        netInBuffer.flip();
                    }
                    break;
                case NEED_WRAP:
                    netOutBuffer.clear();
                    wrap(emptyBuff, netOutBuffer);
                    netOutBuffer.flip();
                    while(netOutBuffer.hasRemaining()) {
                        session.writeToChannel(netOutBuffer);
                    }
                    break;
                case NEED_TASK:
                    Runnable run;
                    while((run = engine.getDelegatedTask()) != null) {
                        run.run();
                    }
                    break;
                case FINISHED:
                case NOT_HANDSHAKING:
                    //free buffer
                    session.putWriteCacheBuffer(appInBuffer);
                    session.putWriteCacheBuffer(netOutBuffer);
                    appInBuffer = null;
                    netOutBuffer = null;
                    if(netInBuffer.hasRemaining()) {
                        netInBuffer.compact();
                        return netInBuffer;
                    }
                    session.putWriteCacheBuffer(netInBuffer);
                    netInBuffer = null;
                    return null;
            }
            status = engine.getHandshakeStatus();
        } while(true);
    }

    /**
     *
     * @param in
     * @param out
     * @return 返回false表示需要读取更多数据
     * @throws Exception
     */
    public boolean unwrap(ByteBuffer in, ByteBuffer out) throws Exception {

       Status status = engine.unwrap(in, out).getStatus();
       if(status == Status.BUFFER_UNDERFLOW) {
           return false;
       }
       if(status != Status.OK) {
           throw new Exception("unwrap error:" + status + " in:" + in + " out:" + out);
       }

       return true;
    }


    public Status wrap(ByteBuffer in, ByteBuffer out) throws Exception {
        SSLEngineResult res = engine.wrap(in, out);
        Status status = res.getStatus();
        if(status == Status.BUFFER_OVERFLOW) {
            throw new Exception(" wrap error:" + status + " in:" + in + " out:" + out);
        }
        if(status == Status.CLOSED) {
            throw new Exception(" wrap error:" + status + " in:" + in + " out:" + out);
        }
        return status;
    }

    public void close() {
        if(engine != null) {
            engine.closeOutbound();
            try {
                engine.closeInbound();
                engine.getSession().invalidate();
            } catch (Exception e) {}
        }
    }


    public int getAppBufferSize() {
        return appBufferSize;
    }

    public int getNetBufferSize() {
        return netBufferSize;
    }
}
