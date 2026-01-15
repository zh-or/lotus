package or.lotus.core.nio.http;

/** http协议异常或者超过限制 */
public class HttpServerException extends RuntimeException {
    int httpCode;
    HttpRequest request;

    public HttpServerException(int httpCode, HttpRequest request, String message) {
        super(message);
        this.request = request;
        this.httpCode = httpCode;
    }

    public HttpServerException(int httpCode, HttpRequest request, Throwable cause) {
        super(cause);
        this.request = request;
        this.httpCode = httpCode;
    }
}
