package or.lotus.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpResponsePkg {
    FullHttpResponse response;
    File file;

    private HttpResponsePkg() {
    }

    public static HttpResponsePkg create(File file) throws ParseException, IOException {
        HttpResponsePkg pkg = new HttpResponsePkg();
        pkg.file = file;

        return pkg;
    }


    public static HttpResponsePkg create() {
        return create(HttpResponseStatus.OK);
    }

    public static HttpResponsePkg create(HttpResponseStatus status) {
        HttpResponsePkg pkg = new HttpResponsePkg();
        pkg.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER);
        return pkg;
    }

    public static HttpResponsePkg create(HttpRequestPkg req, ByteBuf buf) {
        return create(HttpResponseStatus.OK, req, buf);
    }

    public static HttpResponsePkg create(HttpResponseStatus status, HttpRequestPkg req, String content) {
        ByteBuf buf =  req.channelCtx.alloc().heapBuffer(req.context.responseBufferSize);
        buf.writeCharSequence(content, req.context.charset);
        return create(status, req, buf);
    }

    public static HttpResponsePkg create(HttpResponseStatus status, HttpRequestPkg req, ByteBuf buf) {
        HttpResponsePkg pkg = new HttpResponsePkg();
        pkg.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
        return pkg;
    }
    public static HttpResponsePkg create(HttpRequestPkg req, byte[] bytes) {
        HttpResponsePkg pkg = new HttpResponsePkg();
        ByteBuf buf =  req.channelCtx.alloc().heapBuffer(bytes.length);
        buf.writeBytes(bytes);
        pkg.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        return pkg;
    }


    public static HttpResponsePkg create(HttpRequestPkg req, String content) {
        ByteBuf buf =  req.channelCtx.alloc().heapBuffer(req.context.responseBufferSize);
        buf.writeCharSequence(content, req.context.charset);
        return create(req, buf);
    }

    public static HttpResponsePkg redirect(String newUri) {
        HttpResponsePkg pkg = new HttpResponsePkg();
        pkg.response = new DefaultFullHttpResponse(HTTP_1_1, FOUND, Unpooled.EMPTY_BUFFER);
        pkg.response.headers().set(HttpHeaderNames.LOCATION, newUri);
        return pkg;
    }

    public FullHttpResponse raw() {

        return response;
    }


    public boolean isFileResponse() {
        return file != null;
    }
}
