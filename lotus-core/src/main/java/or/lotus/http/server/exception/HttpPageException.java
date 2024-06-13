package or.lotus.http.server.exception;

import or.lotus.http.ApiRes;

public class HttpPageException  extends Exception{
    public String templateName;

    public HttpPageException(String templateName, String message) {
        super(message);
        this.templateName = templateName;
    }
}
