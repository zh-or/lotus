package or.lotus.core.http.restful.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import or.lotus.core.http.restful.RestfulResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NettyResponse extends RestfulResponse {
    NettyRequest request;
    private FullHttpResponse response = null;
    public NettyResponse(NettyRequest request) {
        super(request);
        this.request = request;

    }

    public FullHttpResponse getResponse() {
        if(response == null) {
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(status.code()),
                    bodyBuffer == null ? Unpooled.EMPTY_BUFFER : bodyBuffer
            );
        }
        HttpHeaders nh = response.headers();
        for(Map.Entry<String, String> entry : headers.entrySet()) {
            nh.set(entry.getKey(), entry.getValue());
        }

        return response;
    }

    private ByteBuf bodyBuffer;
    private ByteBuf getBuffer() {
        if(bodyBuffer == null) {
            bodyBuffer = request.channel.alloc().heapBuffer(request.getContext().getBufferSize());
        }
        return bodyBuffer;
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        getBuffer();
        bodyBuffer.writeCharSequence(str.subSequence(off, off + len), charset);
    }

    @Override
    public void write(int c) throws IOException {
        getBuffer();
        bodyBuffer.writeByte(c);
    }

    @Override
    public RestfulResponse write(byte[] data) {
        getBuffer();
        bodyBuffer.writeBytes(data);
        return this;
    }

    @Override
    public RestfulResponse clearWrite() {
        if(bodyBuffer != null) {
            bodyBuffer.clear();
        }
        return this;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        getBuffer();
        int max = off + len;
        for(int i = off; i < max; i ++ ) {
            bodyBuffer.writeByte(cbuf[i]);
        }
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

}
