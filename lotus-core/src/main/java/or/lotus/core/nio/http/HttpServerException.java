package or.lotus.core.nio.http;

public class HttpServerException extends Exception {
    int httpCode;

    public HttpServerException(int httpCode, String message) {
        super(message);
        this.httpCode = httpCode;
    }
}
