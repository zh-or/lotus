package or.lotus.http.netty.exception;

public class NettyHttpPageException extends RuntimeException{
    public String templateName;

    public NettyHttpPageException(String templateName, String message) {
        super(message);
        this.templateName = templateName;
    }
}
