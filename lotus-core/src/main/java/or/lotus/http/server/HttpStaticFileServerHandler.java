package or.lotus.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static or.lotus.http.server.LotusResponseSender.sendError;
import static or.lotus.http.server.LotusResponseSender.sendFile;

/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="https://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 *
 * <h3>How Browser Caching Works</h3>
 *
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of {@code /file1.txt}.</li>
 * <li>Contents of {@code /file1.txt} is cached by the browser.</li>
 * <li>Request #2 for {@code /file1.txt} does not return the contents of the
 *     file again. Rather, a 304 Not Modified is returned. This tells the
 *     browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 *     {@code If-Modified-Since} date is the same as the file's last
 *     modified date.</li>
 * </ol>
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
public class HttpStaticFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    protected static Logger log = LoggerFactory.getLogger(HttpStaticFileServerHandler.class);


    private HttpServer context;

    public HttpStaticFileServerHandler(HttpServer context) {
        this.context = context;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(context, ctx, request, BAD_REQUEST);
            return;
        }

        if (!GET.equals(request.method())) {
            sendError(context, ctx, request, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = request.uri();


        final String path = sanitizeUri(uri, context.charset.displayName());
        if (path == null) {
            sendError(context, ctx, request, FORBIDDEN);
            return;
        }


        sendFile(context, ctx, request, path);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //文件这边不报异常, 统一404
        log.error("HttpStaticFileServerHandler.java:242 error:", cause);
        if (ctx.channel().isActive()) {
            sendError(context, ctx, null, NOT_FOUND);
        }
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri, String charset) {
        // Decode the path.
        try {
            URI uri2 = new URI(uri);
            uri = URLDecoder.decode(uri2.getPath(), charset);

            if("/".equals(uri)) {
                uri = context.defaultIndexFile;
            }

        } catch (Exception e) {
            return null;
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        // Convert file separators.
        uri = uri.replace('/', File.separatorChar);



        // Convert to absolute path.
        return context.staticPath + File.separator +  Utils.BuildPath(uri);
    }



}
