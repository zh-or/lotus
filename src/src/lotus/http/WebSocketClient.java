package lotus.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import lotus.utils.Base64;
import lotus.utils.Utils;

public class WebSocketClient {

    public static void main(String[] args) throws IOException, URISyntaxException {
        
        /**
         * 
         * ws://121.40.165.18:8800
         * 
         * wss://api.zb.cn/websocket
         */
        WebSocketClient ws = WebSocketClient.connection(new URI("wss://api.zb.cn/websocket"),
                new Proxy(Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080)),
                new WsHandler() {

            @Override
            public void onRecv(WebSocketClient ws, WsResponse response) {
                System.out.println("recv:" + new String(response.getBody()));
                Utils.SLEEP(1100);
                ws.send(WsRequest.text("date:" + System.currentTimeMillis()));
            }

            @Override
            public void onClose(WebSocketClient ws) {
                System.out.println("连接断开?");
            }

            @Override
            public void onConn(WebSocketClient ws) {
                System.out.println("连接完成...");
                ws.send(WsRequest.text("{\"event\":\"addChannel\", \"channel\":\"markets\"}\n").mask());
            }

        });
        //ws.send(WsRequest.text("test"));

        
    }

    private static final String fistr_package = 
                    "GET %s HTTP/1.1\r\n" + 
                    "Host: %s\r\n" + 
                    "Connection: Upgrade\r\n" + 
                    "Upgrade: websocket\r\n" +
                    "Pragma: no-cache\r\n" + 
                    "Cache-Control: no-cache\r\n" + 
                    "Sec-WebSocket-Version: 13\r\n" + 
                    "Sec-WebSocket-Key: %s\r\n\r\n";

    private Socket c;
    private WsHandler handler;
    private InputStream cIn;
    private OutputStream cOut;
    private long rand;
    private int readCacheLen = 1024 * 4;
    private boolean run = false;

    private CountDownLatch cdWaitThreadQuit;

    private LinkedBlockingQueue<WsRequest> qSend = null;

    
    private static class TrustAnyTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }
    
    private WebSocketClient(URI uri, WsHandler handler, Proxy proxy) throws IOException {
        int port = uri.getPort();
        switch (uri.getScheme()) {
            case "http":
            case "ws":
                port = port == -1 ? 80 : port;
                if(proxy != null) {
                    this.c = new Socket(proxy);
                }else {
                    this.c = new Socket();
                }
                this.c.connect(new InetSocketAddress(uri.getHost(), port));
                break;
            case "https":
            case "wss":
                port = port == -1 ? 443 : port;
                try {
                    SSLContext ctx = SSLContext.getInstance("SSL");
                    ctx.init(null, new TrustAnyTrustManager[] { new TrustAnyTrustManager()}, new SecureRandom());
                    if(proxy != null) {
                        Socket tSock = new Socket(proxy);
                        tSock.connect(new InetSocketAddress(uri.getHost(), port));
                        this.c = ctx.getSocketFactory().createSocket(tSock, uri.getHost(), port, false);
                    }else {
                        this.c = ctx.getSocketFactory().createSocket();
                        this.c.connect(new InetSocketAddress(uri.getHost(), port));
                    }
                } catch (NoSuchAlgorithmException e1) {
                    e1.printStackTrace();
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }
                break;
        }
        this.handler = handler;
        this.rand = System.currentTimeMillis();
        this.cIn = this.c.getInputStream();
        this.cOut = this.c.getOutputStream();

        this.c.setSoTimeout(10000);// 读超时时间
        this.c.setReceiveBufferSize(readCacheLen);
        /*this.c.setKeepAlive(false);
        this.c.setTcpNoDelay(false);*/
        this.cdWaitThreadQuit = new CountDownLatch(2);

        String key = Base64.byteArrayToBase64(Utils.RandomNum(16).getBytes());
        String tmp = String.format(fistr_package, uri.getPath(), uri.getHost(), key);
        this.cOut.write(tmp.getBytes());
        this.cOut.flush();
        //Utils.SLEEP(1000);
        // System.out.println("len:" + cIn.available());
        BufferedReader br = new BufferedReader(new InputStreamReader(this.cIn));
        StringBuffer sb1 = new StringBuffer();
        while(true) {
            String line = br.readLine();
            if("".equals(line) || line == null) {
                break;
            }
            sb1.append(line);
            sb1.append("\r\n");
        }
        //int len = this.cIn.read(readCache, 0, readCacheLen);
        String upgrade = sb1.toString();
        String accept = Utils.getMid(upgrade, "Sec-WebSocket-Accept: ", "\n");

        try {
            String selfAccept = Base64.byteArrayToBase64(Utils.SHA1(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"));
            if (selfAccept == null || !selfAccept.equals(accept)) {
                System.out.println(accept);
                System.out.println(selfAccept);
                throw new IOException("握手失败");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("握手失败:" + e.getMessage());
        }
        run = true;
        this.qSend = new LinkedBlockingQueue<>(2000);

        new Thread(new Runnable() {

            private byte readByte() throws IOException {
                int tmp = cIn.read();
                if (tmp == -1) {
                    close();
                }
                return (byte) tmp;
            }

            @Override
            public void run() {
                byte b;
                int packlen, payloadLength;
                while (run) {
                    try {
                        b = readByte();
                        WsResponse res = new WsResponse((b & 0x80) > 0, // fin
                                (b & 0x70) >>> 4, // rsv
                                (byte) (b & 0x0f)// opcode
                        );
                        b = readByte();
                        res.hasMask = (b & 0xFF) >> 7 == 1;
                        payloadLength = b & 0x7F;// 后面7位
                        packlen = 0;
                        if (payloadLength == 126) {
                            packlen = (readByte() & 0xff) << 8;
                            packlen |= (readByte() & 0xff);
                        } else if (payloadLength == 127) {
                            byte[] _long = new byte[8];
                            cIn.read(_long);
                            packlen = (int) Utils.byte2long(_long);
                        }else {
                            packlen = payloadLength;
                        }
                        if (res.hasMask) {
                            byte[] mask = new byte[4];
                            cIn.read(mask);
                            res.mask = mask;
                        }
                        if (packlen > 0) {
                            byte[] body = new byte[packlen];
                            cIn.read(body, 0, packlen);
                            res.body = body;
                        }
                        if(WsStatus.OPCODE_CLOSE == res.op) {
                            cdWaitThreadQuit.countDown();
                            close();
                        }else {
                            WebSocketClient.this.handler.onRecv(WebSocketClient.this, res);
                        }
                        

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                cdWaitThreadQuit.countDown();
            }
        }).start();

        new Thread(new Runnable() {

            @Override
            public void run() {
                WebSocketClient.this.handler.onConn(WebSocketClient.this);
                while (run) {
                    WsRequest req = null;
                    try {
                        req = qSend.poll(500, TimeUnit.MICROSECONDS);
                        if (req != null && run) {
                            int len = 0;
                            
                            if(req.body != null) {
                                len += req.body.length;
                            }
                            cOut.write(0x8f & req.op);
                            boolean hasMask = req.hasMask;
                            if(len < 126) {
                                cOut.write(hasMask ? len & 0x80 : len);
                            }else if(len < 65535) {
                                cOut.write(hasMask ? 126 & 0x80 : 126);
                                cOut.write(len >>> 8);
                                cOut.write(len & 0xff);
                            }else {
                                cOut.write(hasMask ? 127 & 0x80 : 127);
                                byte[] _long = Utils.long2byte(len);
                                cOut.write(_long);
                            }
                            if(hasMask) {
                                cOut.write(req.mask);
                            }
                            if(req.body != null && req.body.length > 0) {
                                if(hasMask) {
                                    int pLen = req.body.length;
                                    for(int i = 0; i < pLen; i++){
                                        req.body[i] = (byte) (req.body[i] ^ req.mask[i % 4]);
                                    }
                                }
                                cOut.write(req.body, 0, len);
                            }
                            cOut.flush();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        cdWaitThreadQuit.countDown();
                        close();
                        e.printStackTrace();
                    }
                }
                cdWaitThreadQuit.countDown();
            }
        }).start();
    }

    /**
     * 调用时会等待发送线程/接收线程退出, 最多等待10S如线程没有退出当前方法会直接返回
     */
    public void close() {
        run = false;
        try {
            this.cIn.close();
        } catch (IOException e) {
        }
        try {
            this.cOut.close();
        } catch (IOException e) {
        }
        try {
            this.c.close();
        } catch (IOException e) {
        }
        try {
            cdWaitThreadQuit.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        handler.onClose(this);
    }

    public static WebSocketClient connection(URI uri, WsHandler handler) throws IOException {
        return connection(uri, null, handler);
    }
    
    public static WebSocketClient connection(URI uri, Proxy proxy, WsHandler handler) throws IOException {
        return new WebSocketClient(uri, handler, proxy);
    }
    
    public void send(WsRequest req) {
        qSend.add(req);
    }

    public interface WsHandler {
        public void onConn(WebSocketClient ws);
        
        public void onRecv(WebSocketClient ws, WsResponse response);

        public void onClose(WebSocketClient ws);
    }
}
