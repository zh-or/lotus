package lotus.http.server.support;

import java.io.IOException;
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
    public final static  String     SSL_STATE_KEY          =   "___SSL_STATE_KEY___";
    private static final ByteBuffer emptyBuff              =   ByteBuffer.allocate(0);
    private HttpServer      server;
    private SSLEngine       engine;
    private NioTcpSession   session;
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
        //发个空数据
        session.write(emptyBuff);// initializes res
        
        do {
            System.out.println("status:" + status);
            switch(status) {
                case NEED_TASK:
                    Runnable run;
                    while((run = engine.getDelegatedTask()) != null) {
                        run.run();
                    }
                    break;
                case NEED_UNWRAP:
                case NEED_UNWRAP_AGAIN:
                    do {
                        netInBuffer.flip();
                        SSLEngineResult res = engine.unwrap(netInBuffer, appInBuffer);
                        Status tStatus = res.getStatus();
                        netInBuffer.compact();
                        if(tStatus == Status.OK) {
                            //appInBuffer.rewind();
                            //session.write(appInBuffer);
                            System.out.println("unwrap ok:" + netInBuffer);
                            break;
                        } else if(tStatus == Status.BUFFER_UNDERFLOW) {
                            session.read(netInBuffer);
                        } else if(tStatus == Status.BUFFER_OVERFLOW) {
                            throw new SSLException("unwrap BUFFER_OVERFLOW");
                        } else if(tStatus == Status.CLOSED) {
                            throw new SSLClosedException();
                        }
                        
                    } while(true);
                    break;
                case NEED_WRAP:
                    do {
                        appInBuffer.clear();
                        netOutBuffer.clear();
                        SSLEngineResult res = engine.wrap(appInBuffer, netOutBuffer);
                        Status tStatus = res.getStatus();
                        if(tStatus == Status.OK) {
                            netOutBuffer.flip();
                            session.write(netOutBuffer);
                            break;
                        } else if(tStatus == Status.BUFFER_UNDERFLOW || tStatus == Status.BUFFER_OVERFLOW) {
                            throw new SSLException("BUFFER_UNDERFLOW || BUFFER_OVERFLOW");
                        } else if(tStatus == Status.CLOSED) {
                            throw new SSLClosedException();
                        }
                    } while(true);
                    
                    
                    break;
                case FINISHED:
                    System.out.println("握手完成1:" + netInBuffer);
                case NOT_HANDSHAKING:
                    System.out.println("握手完成2:" + netInBuffer);
                    //netInBuffer.flip();
                    return netInBuffer;
            }
            status = engine.getHandshakeStatus();
        } while(true);
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
    

    public int getAppBufferSize() {
        return appBufferSize;
    }

    public int getNetBufferSize() {
        return netBufferSize;
    }
}
