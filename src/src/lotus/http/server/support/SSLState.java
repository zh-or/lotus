package lotus.http.server.support;

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

import lotus.http.server.HttpServer;
import lotus.nio.tcp.NioTcpSession;

public class SSLState {
    public final static String     SSL_STATE_KEY          =   "___SSL_STATE_KEY___";

    private HttpServer      server;
    private SSLEngine       engine;
    private NioTcpSession   session;
    private ByteBuffer      appInBuffer;
    private ByteBuffer      netInBuffer;
    private ByteBuffer      netOutBuffer;
    
    private int             appBufferSize;
    private int             netBufferSize;
    
    public enum SelfHandhakeState {
        NEED_DATA,
        NEED_SEND,
        FINISHED
    }
    
    public SSLState(HttpServer server, NioTcpSession session) throws SSLException {
        this.server = server;
        this.session = session;
        SSLContext ssl = server.getSSLContext();
        InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
        engine = ssl.createSSLEngine(address.getHostString(), address.getPort());
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
        this.server.reSizeCacheBuffer(appBufferSize + 50);
        
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
    
    public SelfHandhakeState doHandshake(ByteBuffer netIn, ByteBuffer surplus) throws SSLException {

        HandshakeStatus status = engine.getHandshakeStatus();
        System.out.println("status ->" + status.toString());
        switch(status) {
            case NEED_TASK:
                Runnable run;
                while((run = engine.getDelegatedTask()) != null) {
                    run.run();
                }
                return doHandshake(netIn, surplus);
                
            case NEED_WRAP:
            {
                //netOutBuffer.clear();
                appInBuffer.flip();
                SSLEngineResult res = engine.wrap(appInBuffer, netOutBuffer);
                appInBuffer.compact();
                Status state = res.getStatus();
                if(state == Status.BUFFER_UNDERFLOW || state == Status.BUFFER_OVERFLOW) {
                    throw new SSLException("NEED_WRAP -> BUFFER_UNDERFLOW | BUFFER_OVERFLOW:" + state);
                   
                } else if(state == Status.OK) {
                    //netOutBuffer.flip();
                    
                    session.write(new HttpMessageWrap(HttpMessageWrap.HTTP_MESSAGE_HTTPS_HANDHAKE, netOutBuffer));
                    netOutBuffer = session.getWriteCacheBuffer(netBufferSize);
                    netOutBuffer.clear();
                    HandshakeStatus tmpStatus = engine.getHandshakeStatus();
                    if(tmpStatus != HandshakeStatus.NEED_UNWRAP) {
                        return doHandshake(null, surplus);
                    }
                    return SelfHandhakeState.NEED_SEND;
                } else if(state == Status.CLOSED) {
                    throw new SSLException("CLOSED:" + status);
                }
            }
                
                break;
            case NEED_UNWRAP:
            //case NEED_UNWRAP_AGAIN://jdk8+新增
            {
                if(netIn != null) {
                    netInBuffer.put(netIn);
                }
                netInBuffer.flip();
                String  before = netInBuffer.toString();
                System.out.println("netInBuffer->" + before );
            
                SSLEngineResult res = engine.unwrap(netInBuffer, appInBuffer);
                int remaining = netInBuffer.remaining();
                //压缩数据
                netInBuffer.compact();
                Status state = res.getStatus();
                System.out.println("netInBuffer->" + before + "->" + netInBuffer.toString() + " remaining:" + remaining + " ->" + state.toString());
                
                
                if(state == Status.BUFFER_UNDERFLOW) {
                    return SelfHandhakeState.NEED_DATA;
                    
                } else if(state == Status.BUFFER_OVERFLOW) {
                    throw new SSLException("BUFFER_OVERFLOW");
                    
                } else if(state == Status.OK) {
                    return doHandshake(null, surplus);
                    
                } else if(state == Status.CLOSED) {
                    throw new SSLException("CLOSED:" + status);
                }
                break;
            }
            case NOT_HANDSHAKING:
                System.out.println("NOT_HANDSHAKING ->" + netInBuffer.toString());
                netInBuffer.flip();
                while(netInBuffer.hasRemaining()) {
                    surplus.put(netInBuffer.get());
                }
            case FINISHED:
                if(appInBuffer != null) {
                    session.putWriteCacheBuffer(appInBuffer);
                    appInBuffer = null;
                }
                session.putWriteCacheBuffer(netInBuffer);
                netInBuffer = null;
                return SelfHandhakeState.FINISHED;
        }
        
        return SelfHandhakeState.FINISHED;
    }
    
    public void unwrap(ByteBuffer in, ByteBuffer out) throws SSLException, SSLClosedException {
        SSLEngineResult res = engine.unwrap(in, out);
        Status status = res.getStatus();
        if(status == Status.CLOSED) {
            throw new SSLClosedException();
        }
        if(status != Status.OK) {
            throw new SSLException("ssl 解密异常:" + res.getStatus());
        }
        out.flip();
    }
     
    public void wrap(ByteBuffer in, ByteBuffer out) throws SSLException, SSLClosedException {
        SSLEngineResult res = engine.wrap(in, out);
        Status status = res.getStatus();
        if(status == Status.CLOSED) {
            throw new SSLClosedException();
        }
        if(status != Status.OK) {
            throw new SSLException("ssl 加密异常:" + res.getStatus());
        }
    }
    
    public void free() {
        if(engine != null) {
            try {
                engine.closeInbound();
            } catch (SSLException e) {}
            engine.closeOutbound();
        }
    }
    
}
