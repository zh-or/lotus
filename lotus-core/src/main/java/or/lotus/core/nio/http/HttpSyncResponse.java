package or.lotus.core.nio.http;

import or.lotus.core.nio.LotusByteBuf;
import or.lotus.core.nio.tcp.NioTcpSession;

public class HttpSyncResponse implements AutoCloseable {
    protected NioTcpSession session;
    protected HttpRequest request;
    protected LotusByteBuf buffer;
    protected boolean isEnd = false;

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

    public synchronized void flush() {
        if(buffer != null && buffer.getDataLength() > 0) {
            HttpSyncResponse obj = new HttpSyncResponse(session, request);
            session.write(obj);
            obj.buffer = buffer;
            buffer = null;
        }
    }

    public void syncEnd() {
        write("0\r\n\r\n");
        flush();
        isEnd = true;
    }


    private void checkBuffer() {
        if(buffer == null) {
            buffer = request.getContext().server.pulledByteBuffer();
        }
    }

    @Override
    public synchronized void close() throws Exception {
        flush();
    }
}
