package or.lotus.core.http.server.exception;

public class HttpPageException  extends Exception{
    public String templateName;

    public HttpPageException(String templateName, String message) {
        super(message);
        this.templateName = templateName;
    }
}
