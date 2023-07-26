package lotus.http.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpResponsePkg {
    FullHttpResponse response;

    private HttpResponsePkg() {
    }


    public static HttpResponsePkg create() {
        HttpResponsePkg pkg = new HttpResponsePkg();;
        pkg.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        return pkg;
    }

    public static HttpResponsePkg create(HttpRequestPkg req, String content) {
        HttpResponsePkg pkg = new HttpResponsePkg();
        ByteBuf buf =  req.channelCtx.alloc().heapBuffer();
        buf.writeCharSequence(content.toString(), req.context.charset);
        pkg.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        return pkg;
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
}
