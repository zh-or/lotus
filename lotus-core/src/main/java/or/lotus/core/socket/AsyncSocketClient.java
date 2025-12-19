package or.lotus.core.socket;

import or.lotus.core.common.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import java.lang.AutoCloseable;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 异步socket
 */
public class AsyncSocketClient implements AutoCloseable {
    private Socket socket = null;
    private ClientCallback callback = null;
    private long lastActiveTime = 0;
    private int idleTime = 0;
    private Object attr = null;//辅助参数
    private int receiveBufferSize = 1024 * 2;
    private int readTimeout = 1000;
    private volatile boolean isRunning = true;
    protected Thread sendThread;
    protected Thread receiveThread;
    protected LinkedBlockingQueue<byte[]> msgList = null;

    public AsyncSocketClient(ClientCallback callback) {
        this.callback = callback;
        msgList = new LinkedBlockingQueue<>();
    }

    public Object getAttr() {
        return attr;
    }

    public void setReadTimeOut(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }

    public void setReceiveBufferSize(int size) {
        this.receiveBufferSize = size;
    }

    /**
     * @param serverAddress
     * @param timeout       连接超时时间
     * @return
     */
    public synchronized boolean connection(InetSocketAddress serverAddress, int timeout) {
        close();
        try {

            socket = new Socket();
            socket.setReceiveBufferSize(receiveBufferSize);
            lastActiveTime = System.currentTimeMillis();
            socket.setSoTimeout(readTimeout);
            socket.connect(serverAddress, timeout);

            isRunning = true;
            sendThread = new Thread(new SendRunnable());
            receiveThread = new Thread(new ReceiveRunnable());
            sendThread.setName("lotus async socket client send thread");
            receiveThread.setName("lotus async socket client receive thread");
            sendThread.start();

            return true;
        } catch (Exception e) {
        }

        return false;
    }

    private class ReceiveRunnable implements Runnable {
        @Override
        public void run() {
            byte[] head = new byte[3];

            do {
                try {
                    InputStream in = socket.getInputStream();
                    readByInputStream(in, head);

                    if (head[0] != 0x02) {
                        throw new IOException("头部错误 head[0] != 0x02");
                    }
                    int len = Utils.byte2short(head, 1);
                    if (len < 65535) {
                        len -= 4;
                        byte[] content = new byte[len];
                        readByInputStream(in, content);
                        if (in.read() != 0x03) {
                            throw new IOException("尾部错误 != 0x02");
                        }
                        callback.onMessageReceive(AsyncSocketClient.this, content);
                        lastActiveTime = System.currentTimeMillis();
                    }
                } catch (SocketTimeoutException e) {
                    //超时不管
                } catch (Exception e) {
                    callback.onException(AsyncSocketClient.this, e);
                    close();
                    return;
                }
                if(isRunning && idleTime > 0 && System.currentTimeMillis() - lastActiveTime > idleTime) {
                    callback.onIdle(AsyncSocketClient.this);
                }
            } while(isRunning);

        }


        private void readByInputStream(InputStream in, byte[] data) throws Exception {
            int readLen = 0;
            int len = data.length;
            do {
                readLen += in.read(data, readLen, len - readLen);
            } while(readLen < len);
        }
    }

    private class SendRunnable implements Runnable {
        @Override
        public void run() {
            byte[] msg = null;
            do {
                try {
                    msg = msgList.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {}
                if(isRunning && msg != null) {
                    try {
                        OutputStream out = socket.getOutputStream();
                        int len = msg.length + 4;
                        out.write(0x02);
                        out.write(Utils.short2byte(len));
                        out.write(msg);
                        out.write(0x03);
                        out.flush();
                        callback.onSent(AsyncSocketClient.this, msg);
                        lastActiveTime = System.currentTimeMillis();
                    } catch (Exception e) {
                        callback.onException(AsyncSocketClient.this, e);
                        close();
                        return;
                    }
                }
            } while(isRunning && socket != null && !socket.isOutputShutdown());
        }
    }

    public void send(final byte[] data) {
        if(isRunning && data != null) {
            msgList.add(data);
        }
    }

    @Override
    public synchronized void close() {
        if (!isRunning) return;
        isRunning = false;

        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (Exception e) {}
        }
        msgList.clear();

        if(sendThread != null) {
            sendThread.interrupt();
            try {
                if (sendThread.isAlive()) {
                    sendThread.join(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if(receiveThread != null) {
            receiveThread.interrupt();
            try {
                if (receiveThread.isAlive()) {
                    receiveThread.join(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        callback.onClose(this);
    }

    public boolean isConnected() {
        if (socket != null && socket.isConnected() && isRunning)
            return true;
        return false;
    }

    public abstract class ClientCallback {

        /** 链接断开时调用 */
        public void onClose(AsyncSocketClient sc) {
        }

        /** 空闲事件 */
        public void onIdle(AsyncSocketClient sc) {
        }

        /** 收到消息时调用 */
        public void onMessageReceive(AsyncSocketClient sc, byte[] msg) {
        }

        /** 消息发送后调用 */
        public void onSent(AsyncSocketClient sc, byte[] msg) {
        }

        public void onException(AsyncSocketClient sc, Exception e) {
        }
    }

}
