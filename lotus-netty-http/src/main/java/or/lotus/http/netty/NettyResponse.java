package or.lotus.http.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import or.lotus.core.http.restful.RestfulResponse;

import java.io.*;
import java.util.Map;

import static java.lang.Character.*;

public class NettyResponse extends RestfulResponse {
    NettyRequest request;
    private HttpResponse response = null;
    public NettyResponse(NettyRequest request) {
        super(request);
        this.request = request;

    }

    public HttpResponse getResponse() {
        if(response == null) {
            if(isFileResponse()) {
                response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            } else {
                response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.valueOf(status.code()),
                        bodyBuffer == null ? Unpooled.EMPTY_BUFFER : bodyBuffer
                );
            }
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

        bodyBuffer.writeCharSequence(String.valueOf((char) c), charset);
        //bodyBuffer.writeChar(c); //字符集不对
        /*System.out.println("c:" + c + " -> " + Character.isBmpCodePoint(c));*/
        //下面这种有乱码
        /*if (Character.isBmpCodePoint(c))
            bodyBuffer.writeByte( c);
        else {
            bodyBuffer.writeByte((char) ((c >>> 10) + (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT >>> 10))));
            bodyBuffer.writeByte((char) ((c & 0x3ff) + MIN_LOW_SURROGATE));
        }*/
        //Character.toSurrogates(c, v, j++);
        // bodyBuffer.writeByte( c);
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
