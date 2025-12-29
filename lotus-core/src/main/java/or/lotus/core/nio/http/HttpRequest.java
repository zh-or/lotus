package or.lotus.core.nio.http;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.support.RestfulHttpMethod;
import or.lotus.core.nio.tcp.NioTcpSession;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/** HttpRequest 将在调用后自动销毁, body 等参数将无法异步获取 */
public class HttpRequest extends RestfulRequest {
    protected NioTcpSession session;
    protected HashMap<String, String> headers;
    protected RestfulHttpMethod method;
    protected HttpVersion version;
    protected String rawPath;
    protected String path;
    protected String queryString;
    protected boolean isWebSocket = false;
    protected boolean isRewrited = false;
    protected HttpBodyData bodyData;
    protected long contentLength;

    public HttpRequest(HttpServer context, NioTcpSession session, String headerString) {
        super(context);
        this.session = session;
        headers = new HashMap<>();
        final String[] headerFields = headerString.split("\r\n");
        if(headerFields.length > 0) {
            for(String line : headerFields) {
                final String [] kv = line.split(": ");
                if(kv.length >= 2) {
                    headers.put(kv[0].toLowerCase(), kv[1].trim());
                }
            }

            final String[] elements = headerFields[0].split(" ");
            if(elements.length == 3) {
                method = RestfulHttpMethod.byName(elements[0]);
                rawPath = elements[1];
                int mid = rawPath.lastIndexOf("?");
                if(mid != -1) {
                    queryString = rawPath.substring(mid + 1, rawPath.length());
                    path = rawPath.substring(0, mid);
                } else {
                    path = rawPath;
                }

                if(elements[2].indexOf("HTTP/1.1") != -1) {
                    version = HttpVersion.HTTP_1_1;
                }else if(elements[2].indexOf("HTTP/1.0") != -1) {
                    version = HttpVersion.HTTP_1_0;
                }
            }

            if(context.isEnableWebSocket()) {
                String connection = getHeader(HttpHeaderNames.CONNECTION);
                if(!Utils.CheckNull(connection) &&  connection.indexOf("Upgrade") != -1
                    /*这里可能是多个值, 如 FireFox-> Connection:keep-alive, Upgrade*/
                ) {
                    String Upgrade = getHeader(HttpHeaderNames.UPGRADE);
                    if("websocket".equals(Upgrade)) {//websocket 协议
                        isWebSocket = true;
                    }
                }
            }
        }
        contentLength = Utils.tryLong(getHeader(HttpHeaderNames.CONTENT_LENGTH), 0);

    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isWebSocket() {
        return isWebSocket;
    }

    @Override
    public boolean isMultipart() {
        String mimeType = headers.get(HttpHeaderNames.CONTENT_TYPE);
        if (mimeType != null && mimeType.startsWith("multipart/form-data")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isRewriteUrl() {
        return isRewrited;
    }

    @Override
    public void handledRewrite() {
        isRewrited = false;
    }

    @Override
    public void rewriteUrl(String url) {
        int mid = url.lastIndexOf("?");
        if(mid != -1) {
            queryString = url.substring(mid + 1, rawPath.length());
            path = url.substring(0, mid);
        } else {
            path = url;
        }
        isRewrited = true;
    }

    public void setBodyData(HttpBodyData bodyData) {
        this.bodyData = bodyData;
    }

    @Override
    public String getBodyString() {
        if(bodyData != null) {
            return bodyData.getBodyString();
        }
        return null;
    }

    @Override
    public HttpBodyData getBodyFormData() {
        return bodyData;
    }

    @Override
    public String rawPath() {
        return rawPath;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getUrl() {
        return path + (!Utils.CheckNull(queryString) ? "?" : "") + queryString;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public RestfulHttpMethod getMethod() {
        return method;
    }

    @Override
    public HttpServer getContext() {
        return (HttpServer) context;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public String getHeaders() {
        StringBuilder sb = new StringBuilder(2048);
        for(Map.Entry<String, String> item : headers.entrySet()) {
            sb.append(item.getKey());
            sb.append(": ");
            sb.append(item.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return session.getRemoteAddress();
    }

    @Override
    public synchronized void close() {
        if(bodyData != null) {
            bodyData.close();
            bodyData = null;
        }
    }
}
