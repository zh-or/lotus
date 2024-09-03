package or.lotus.core.http.rest;

public abstract class RestfulRequest {

    /**当前是否Multipart请求*/
    public boolean isMultipart() {
        return false;
    }
}
