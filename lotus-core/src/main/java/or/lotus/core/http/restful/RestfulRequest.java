package or.lotus.core.http.restful;

public abstract class RestfulRequest {

    public abstract String getUrl();

    public abstract RestfulHttpMethod getMethod();

    public abstract String getHeader(String name);

    public String getDispatchUrl() {
        return getUrl() + getMethod().name();
    }

    /**当前是否Multipart请求*/
    public boolean isMultipart() {
        return false;
    }
}
