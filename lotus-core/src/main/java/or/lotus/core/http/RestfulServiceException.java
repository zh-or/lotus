package or.lotus.core.http;

/** service 内部使用此异常 */
public class RestfulServiceException extends RuntimeException {

    public RestfulServiceException(String msg) {
        super(msg);
    }


    public RestfulServiceException(String msg, Exception e) {
        super(msg, e);
    }
}
