package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.nio.LotusByteBuffer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class HttpResponse extends RestfulResponse {
    protected HttpRequest request;
    protected boolean isSendHeader = false;
    protected boolean isOpenSync = false;
    protected LotusByteBuffer writeBuffer;
    protected HttpSyncResponse syncResponse;

    public HttpResponse(HttpRequest request) {
        super(request);
        String connection = request.getHeader(HttpHeaderNames.CONNECTION);
        if(connection != null) {
            setHeader(HttpHeaderNames.CONNECTION, connection);
        }
        //todo 处理websocket头
        this.request = request;
    }

    /** 开启异步发送, 异步发送必须调用 HttpSyncResponse.flush() 方法才会发送数据 */
    public HttpSyncResponse openSync() {
        if(file != null) {
            throw new RuntimeException("发送文件不支持再异步!");
        }
        if(syncResponse != null) {
            return syncResponse;
        }
        isOpenSync = true;
        //headers.put("Content-Encoding", "gzip");
        headers.put(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        syncResponse = new HttpSyncResponse(request.session, request);
        if(writeBuffer != null) {
            //buffer转移到异步response中
            syncResponse.buffer = writeBuffer;
            writeBuffer = null;
        }
        //统一在HttpServer.sendResponse 中发送response
        //request.session.write(this);
        return syncResponse;
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
        if(isOpenSync) {
            throw new RuntimeException("启用异步返回后, 请使用 HttpSyncResponse 发送数据");
        }
        if(writeBuffer == null) {
            writeBuffer = (LotusByteBuffer) request.getContext().server.pulledByteBuffer();
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
