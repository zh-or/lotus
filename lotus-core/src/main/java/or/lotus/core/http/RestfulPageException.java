package or.lotus.core.http;

public class RestfulPageException extends RuntimeException {
    public String templateName;

    public RestfulPageException(String templateName, String message) {
        super(message);
        this.templateName = templateName;
    }
}
