package or.lotus.core.nio.http;

/** http协议异常或者超过限制 */
public class HttpServerException extends RuntimeException {
    int httpCode;// 用于response返回的code
    HttpRequest request;//请求, 可能为空
    HttpResponse response;

    public HttpServerException(int httpCode, HttpRequest request, HttpResponse response, String message) {
        super(message);
        this.request = request;
        this.response = response;
        this.httpCode = httpCode;
    }

    public HttpServerException(int httpCode, HttpRequest request, HttpResponse response, Throwable cause) {
        super(cause);
        this.request = request;
        this.response = response;
        this.httpCode = httpCode;
    }
}
