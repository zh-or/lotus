package or.lotus.core.nio.http;

import or.lotus.core.nio.LotusByteBuffer;
import or.lotus.core.nio.Session;

/** 如果打开了Response的异步, 必须手动释放, 否则将导致内存泄漏 */
public class HttpSyncResponse implements AutoCloseable {
    protected HttpServer context;
    protected Session session;
    protected LotusByteBuffer buffer;
    protected boolean isEnd = false;
    protected long lastSendData = 0;
    protected boolean isClosed = false;

    public HttpSyncResponse(HttpServer context, Session session) {
        this.context = context;
        this.session = session;
    }

    public HttpSyncResponse write(String data) {
        checkBuffer();
        buffer.append(data.getBytes(context.getCharset()));
        return this;
    }

    public HttpSyncResponse write(byte[] data) {
        checkBuffer();
        buffer.append(data);
        return this;
    }

    /** 发送当前缓冲区的数据, 如果当前未写入数据则效果同 syncEnd() */
    public synchronized void flush() {
        HttpSyncResponse obj = new HttpSyncResponse(context, session);
        if(buffer != null && buffer.getCountPosition() > 0) {
            obj.buffer = buffer;
            buffer = null;
        } else {
            isEnd = true;
        }
        lastSendData = System.currentTimeMillis();
        if(!session.write(obj)) {
            if(obj.buffer != null) {
                obj.buffer.release();
            }
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
        if(isClosed) {
            throw new RuntimeException("当前HttpSyncResponse已被关闭");
        }
        if(buffer == null) {
            buffer = (LotusByteBuffer) context.server.pulledByteBuffer();
        }
    }

    @Override
    public void close() throws Exception {
        if(buffer != null) {
            buffer.release();
            buffer = null;
        }
        isClosed = true;
    }
}
