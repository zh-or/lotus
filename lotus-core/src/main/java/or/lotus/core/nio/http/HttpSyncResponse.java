package or.lotus.core.nio.http;

import or.lotus.core.nio.LotusByteBuffer;
import or.lotus.core.nio.tcp.NioTcpSession;

public class HttpSyncResponse implements AutoCloseable {
    protected NioTcpSession session;
    protected HttpRequest request;
    protected LotusByteBuffer buffer;
    protected boolean isEnd = false;
    protected long lastSendData = 0;

    public HttpSyncResponse(NioTcpSession session, HttpRequest request) {
        this.session = session;
        this.request = request;
    }

    public HttpSyncResponse write(String data) {
        checkBuffer();
        buffer.append(data.getBytes(request.getContext().getCharset()));
        return this;
    }

    public HttpSyncResponse write(byte[] data) {
        checkBuffer();
        buffer.append(data);
        return this;
    }

    /** 发送当前缓冲区的数据, 如果当前未写入数据则效果同 syncEnd() */
    public synchronized void flush() {
        HttpSyncResponse obj = new HttpSyncResponse(session, request);
        if(buffer != null && buffer.getCountPosition() > 0) {
            obj.buffer = buffer;
            buffer = null;
        } else {
            isEnd = true;
        }
        lastSendData = System.currentTimeMillis();
        if(!session.write(obj)) {
            obj.buffer.release();
        }
    }

    /** 发送结束 */
    public void syncEnd() {
        if(buffer != null && buffer.getCountPosition() > 0) {
            flush();
        }
        //write("0\r\n\r\n");
        flush();
    }


    private void checkBuffer() {
        if(buffer == null) {
            buffer = (LotusByteBuffer) request.getContext().server.pulledByteBuffer();
        }
    }

    @Override
    public synchronized void close() throws Exception {
        flush();
    }
}
