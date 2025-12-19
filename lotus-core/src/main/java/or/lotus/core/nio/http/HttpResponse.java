package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.nio.LotusByteBuf;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class HttpResponse extends RestfulResponse {
    protected HttpRequest request;
    protected boolean isSendHeader = false;
    protected boolean isOpenSync = false;
    protected LotusByteBuf writeBuffer;

    public HttpResponse(HttpRequest request) {
        super(request);
        this.request = request;
    }

    public HttpSyncResponse openSync() {
        if(file != null) {
            throw new RuntimeException("发送文件不支持再异步!");
        }
        isOpenSync = true;
        //headers.put("Content-Encoding", "gzip");
        headers.put(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        HttpSyncResponse obj = new HttpSyncResponse(request.session, request);
        request.session.write(this);
        return obj;
    }

    public boolean isOpenSync() {
        return isOpenSync;
    }


    @Override
    public void flush() throws IOException {

    }


    @Override
    public void close() throws IOException {
        if(writeBuffer != null) {
            writeBuffer.release();
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        checkBuffer();
        writeBuffer.append(str.substring(off, off + len).getBytes(charset));
    }

    @Override
    public void write(int c) throws IOException {
        checkBuffer();
        writeBuffer.append(String.valueOf((char) c).getBytes(charset));
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {

        checkBuffer();
        writeBuffer.append(String.valueOf(cbuf, off, len).getBytes(charset));
    }

    @Override
    public HttpResponse write(byte[] data) {
        checkBuffer();
        writeBuffer.append(data);
        return this;
    }

    @Override
    public HttpResponse clearWrite() {
        if(writeBuffer != null) {
            writeBuffer.clear();
        }
        return this;
    }

    private void checkBuffer() {
        if(writeBuffer == null) {
            writeBuffer = request.getContext().server.pulledByteBuffer();
        }
    }

    public String getHeaders() {
        StringBuilder sb = new StringBuilder(request.getContext().getMaxHeaderSize());
        sb.append(status.line());
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, String> item = it.next();
            sb.append(item.getKey());
            sb.append(": ");
            sb.append(item.getValue());
            sb.append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }
}
