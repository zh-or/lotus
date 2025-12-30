package or.lotus.core.nio.http;

public class HttpPageException extends RuntimeException{
    public String templateName;

    public HttpPageException(String templateName, String message) {
        super(message);
        this.templateName = templateName;
    }
}
