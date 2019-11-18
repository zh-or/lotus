package lotus.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import lotus.utils.Base64;
import lotus.utils.Utils;

/**
 * 默认 5 秒没有通讯则发送一次ping包
 * @author or
 *
 */
public class WebSocketClient {
    public interface Handler{

        public void onConn(WebSocketClient ws);
        
        public void onRecv(WebSocketClient ws, WebSocketFrame frame);

        /**
         * 自己调用close方法时, 不会调用此回调
         * @param ws
         */
        public void onClose(WebSocketClient ws);
        
        public void onError(Throwable e);
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
    
    private SSLContext       sslCtx      =   null;
    private Handler          handler     =   null;
    private InputStream      bIn         =   null;
    private OutputStream     bOut        =   null;
    private volatile boolean run         =   false;
    private volatile boolean isConnection=   false;
    private volatile boolean errClose    =   false;
    
    private CountDownLatch                      cdWaitThreadQuit    = null;
    private LinkedBlockingQueue<WebSocketFrame> qSend               = null;
    private Socket                              socket              = null;
    private int                                 ideaTime            = 5000;//超过空闲时间则发送 ping 包
    private long                                lastActiveTime      = 0;
    private Timer                               ideaCheckTimer      = null;
    
    
    private WebSocketClient(URI uri, Handler handler, Proxy proxy) throws Exception {
        this.handler = handler;
        int port = uri.getPort();
        switch (uri.getScheme()) {
            case "http":
            case "ws":
                port = port == -1 ? 80 : port;
                if(proxy != null) {
                    this.socket = new Socket(proxy);
                }else {
                    this.socket = new Socket();
                }
                this.socket.connect(new InetSocketAddress(uri.getHost(), port));
                break;
            case "https":
            case "wss":
                port = port == -1 ? 443 : port;
                sslCtx = SSLContext.getInstance("TLSv1.2");
                try {
                    sslCtx.init(null, null, null);
                    if(proxy != null) {
                        Socket tSock = new Socket(proxy);
                        tSock.connect(new InetSocketAddress(uri.getHost(), port));
                        this.socket = sslCtx.getSocketFactory().createSocket(tSock, uri.getHost(), port, true);
                    }else {
                        this.socket = sslCtx.getSocketFactory().createSocket();
                        this.socket.connect(new InetSocketAddress(uri.getHost(), port));
                    }
                }  catch (KeyManagementException e) {
                    e.printStackTrace();
                }
                break;
        }
        bIn  = socket.getInputStream();
        bOut = socket.getOutputStream();
        socket.setSoTimeout(10000);
        //socket.setReceiveBufferSize(1024 * 4);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(false);
        cdWaitThreadQuit = new CountDownLatch(2);
        
        //握手

        String key = Base64.byteArrayToBase64(Utils.RandomNum(16).getBytes());
        String tmp = String.format(fistr_package, uri.getPath(), uri.getHost(), key);
        
        bOut.write(tmp.getBytes());
        bOut.flush();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(bIn));
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
        String accept = Utils.getMid(upgrade, "Sec-WebSocket-Accept: ", "\r\n");

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
        qSend = new LinkedBlockingQueue<>();
        run = true;
        isConnection = true;
        Thread recv = new Thread(rRecv);
        recv.setName("websocket - recv thread " + this.hashCode());
        recv.start();
        Thread send = new Thread(rSend);
        send.setName("websocket - send thread " + this.hashCode());
        send.start();
        setIdeaTimeDiff(ideaTime);
    }
    
    public static WebSocketClient connection(URI uri, Handler handler) throws Exception {
        return connection(uri, null, handler);
    }
    
    public static WebSocketClient connection(URI uri, Proxy proxy, Handler handler) throws Exception {
        return new WebSocketClient(uri, handler, proxy);
    }
    
    /**
     * 设置连接空闲时间, 超过空闲时间时则发送 ping 包
     * @param t 间隔时间 毫秒
     */
    public void setIdeaTimeDiff(int t){
        this.ideaTime = t;
        if(ideaCheckTimer != null){
            ideaCheckTimer.cancel();
            ideaCheckTimer = null;
        }
        if(t <= 0){
            return ;
        }
        ideaCheckTimer = new Timer("websocket idea timer - " + this.hashCode());
        ideaCheckTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
                 if(lastActiveTime != 0 && System.currentTimeMillis() - lastActiveTime > ideaTime) {
                     if(isConnection()) {
                         send(WebSocketFrame.ping());
                         System.out.println("ping");
                     }
                 }
            }
        }, 500, 500);
    }
    
    
    public void send(WebSocketFrame frame) {
        qSend.add(frame);
    }
    
    /**
     * 调用时会等待发送线程/接收线程退出, 最多等待10S如线程没有退出当前方法会直接返回
     */
    public void close() {
        if(ideaCheckTimer != null){
            ideaCheckTimer.cancel();
            ideaCheckTimer = null;
        }
        run = false;
        try {
            bIn.close();
        } catch (IOException e) {
        }
        try {
            bOut.close();
        } catch (IOException e) {
        }
        try {
            socket.close();
        } catch (IOException e) {
        }
        try {
            cdWaitThreadQuit.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }
    
    public boolean isConnection() {
        return isConnection;
    }

    protected void callClose() {
        //System.out.println("call close:" + cdWaitThreadQuit.getCount());
        if(cdWaitThreadQuit.getCount() <= 0) {
            close();
            if(errClose) {
                handler.onClose(this);
            }
        }
    }
    
    private Runnable rSend = new Runnable() {
        
        @Override
        public void run() {
            handler.onConn(WebSocketClient.this);
            lastActiveTime = System.currentTimeMillis();
            while (run) {
                WebSocketFrame frame = null;
                try {
                    frame = qSend.poll(100, TimeUnit.MICROSECONDS);
                    if (frame != null && run) {
                        lastActiveTime = System.currentTimeMillis();
                        long datalen = (frame.body != null ? frame.body.length : 0);
                        
                        byte b1 = 
                                (byte)( (frame.fin  ? 0x80 : 0x00) | 
                                        (frame.rsv1 ? 0x40 : 0x00) |
                                        (frame.rsv2 ? 0x20 : 0x00) |
                                        (frame.rsv3 ? 0x10 : 0x00)
                                       );
                        b1 = (byte) (b1 | (0x0f & frame.opcode));
                        
                        bOut.write(b1);
                        
                        byte b2 = (byte) (frame.masked ? 0x80 : 0x00);
                        if(datalen < 126) {
                            b2 = (byte) (b2 | datalen);
                            bOut.write(b2);
                        }else if(datalen < 65535) {
                            b2 = (byte) (b2 | 126);
                            bOut.write(b2);
                            //发送2b长度
                            bOut.write((int) (datalen >>> 8));
                            bOut.write((int) (datalen & 0xff));
                        }else {
                            b2 = (byte) (b2 | 127);
                            bOut.write(b2);
                            //发送8b长度
                            bOut.write((int) (datalen & 0xff));
                            bOut.write((int) ((datalen >>> 8) & 0xff));
                            bOut.write((int) ((datalen >>> 16) & 0xff));
                            bOut.write((int) ((datalen >>> 24) & 0xff));
                            bOut.write((int) ((datalen >>> 32) & 0xff));
                            bOut.write((int) ((datalen >>> 40) & 0xff));
                            bOut.write((int) ((datalen >>> 48) & 0xff));
                            bOut.write((int) ((datalen >>> 56) & 0xff));
                        }
                        if(frame.mask != null) {
                            bOut.write(frame.mask);
                        }
                        
                        if(datalen > 0) {
                            if(frame.masked) {
                                int pLen = frame.body.length;
                                for(int i = 0; i < pLen; i++){
                                    frame.body[i] = (byte) (frame.body[i] ^ frame.mask[i % 4]);
                                }
                            }
                            bOut.write(frame.body);
                        }
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                } catch (IOException e) {
                    errClose = true;
                    handler.onError(e);
                    //e.printStackTrace();
                    break;
                }
            }
            isConnection = false;
            cdWaitThreadQuit.countDown();
            callClose();
        }
    };
    
    private Runnable rRecv = new Runnable() {
        
        @Override
        public void run() {
            int  b   = -1;
            long len = 0;
            while (run) {
                try {
                    b = bIn.read();
                    if(b == -1) {
                        errClose = true;
                        break;
                    }
                    WebSocketFrame frame = new WebSocketFrame((byte) (b & 0x0f));
                    frame.fin = (b & 0x80) != 0;
                    frame.rsv1 = (b & 0x40) != 0;
                    frame.rsv2 = (b & 0x20) != 0;
                    frame.rsv3 = (b & 0x10) != 0;
                    
                    b = bIn.read() & 0xff;
                    frame.masked = (b & 0x80) != 0;
                    frame.payload = b & 0x7f;
                    
                    if(frame.payload == 126) {
                        byte[] sizebytes = new byte[3];
                        sizebytes[1] = (byte) bIn.read();
                        sizebytes[2] = (byte) bIn.read();
                        len = new BigInteger(sizebytes).intValue();
                    } else if(frame.payload == 127) {
                        byte[] bytes = new byte[8];
                        for(int i = 0; i < 8; i++) {
                            bytes[i] = (byte) bIn.read();
                        }
                        len = new BigInteger(bytes).longValue();
                    } else {
                        len = frame.payload;
                    }
                    if(frame.masked) {
                        frame.mask = new byte[4];
                        bIn.read(frame.mask);
                    }
                    if(len > 0) {
                        /// 127 就读不出来了 ///
                        frame.body = new byte[(int) len];
                        bIn.read(frame.body);
                    }
                    
                    if(frame != null) {
                        lastActiveTime = System.currentTimeMillis();
                        try {
                            handler.onRecv(WebSocketClient.this, frame);
                        } catch (Exception e) {
                            handler.onError(e);
                        }
                    }
                } catch (SocketTimeoutException ste) {
                    //System.out.println("读超时");
                } catch (IOException e) {
                    errClose = true;
                    handler.onError(e);
                    //e.printStackTrace();
                    break;
                }
            }
            isConnection = false;
            cdWaitThreadQuit.countDown();
            callClose();
        }
    };
}