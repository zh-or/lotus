package or.lotus.core.http.server.exception;

import or.lotus.core.http.ApiRes;

/**在controller内抛出该异常时会将message输出到data字段*/
public class HttpApiException extends Exception{
    public int code;

    public HttpApiException(String message) {
        this(ApiRes.C_ERROR, message);
    }

    public HttpApiException(int code, String message) {
        super(message);
        this.code = code;
    }
}
