package or.lotus.core.socket;

import or.lotus.core.common.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.lang.AutoCloseable;
import java.net.SocketTimeoutException;


/**
 * 同步socket
 *
 * @author OR
 */
public class SyncSocketClient implements AutoCloseable {
    private Socket socket = null;
    private Object attr = null;//辅助参数
    private int receiveBufferSize = 1024 * 2;
    private int receiveTimeout = 10 * 1000;

    public SyncSocketClient() {
    }

    public SyncSocketClient setReceiveTimeOut(int receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
        return this;
    }

    public Object getAttr() {
        return attr;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }

    public void setReceiveBufferSize(int size) {
        this.receiveBufferSize = size;
    }

    /**
     * @param serverAddress
     * @param timeout 连接超时时间
     * @return
     */
    public boolean connection(InetSocketAddress serverAddress, int timeout) {
        close();
        try {
            socket = new Socket();
            socket.setSoTimeout(receiveTimeout);
            socket.setReceiveBufferSize(receiveBufferSize);
            socket.setKeepAlive(false);
            socket.setTcpNoDelay(true);
            socket.connect(serverAddress, timeout);
            return socket.isConnected();
        } catch (Exception e) {
        }

        return false;
    }


    public byte[] sendAndReceive(final byte[] data) throws IOException {
        send(data);
        return receive();
    }

    public void send(final byte[] data) throws IOException {

        if (socket == null || socket.isOutputShutdown()) {
            close();
            throw new IOException("socket closed");
        }
        if (socket.isConnected() == false) {
            close();
            throw new IOException("socket closed");
        }
        try {
            OutputStream out = socket.getOutputStream();
            int len = data.length + 4;
            out.write(0x02);
            out.write(Utils.short2byte(len));
            out.write(data);
            out.write(0x03);
            out.flush();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private byte[] head = new byte[3];

    public byte[] receive() {
        return receive(receiveTimeout);
    }

    public byte[] receive(int timeout) {
        if (socket == null || socket.isInputShutdown()) {
            close();
            return null;
        }
        try {
            socket.setSoTimeout(timeout);
            InputStream in = socket.getInputStream();
            //in.mark(3);
            readByInputStream(in, head);
            if (head[0] == 0x02) {
                int len = Utils.byte2short(head, 1);
                if (len < 65535) {
                    len -= 4;
                    byte[] content = new byte[len];
                    readByInputStream(in, content);
                    if (in.read() == 0x03) {
                        return content;
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            //超时不管
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
        return null;
    }

    private void readByInputStream(InputStream in, byte[] data) throws Exception {
        int readLen = 0;
        int len = data.length;
        do {
            readLen += in.read(data, readLen, len - readLen);
        } while(readLen < len);
    }

    @Override
    public void close() {
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (Exception e) {
            }
        }
    }

    public boolean isConnected() {
        if (socket != null || socket.isConnected())
            return true;
        return false;
    }


}
