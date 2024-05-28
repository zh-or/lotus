package or.lotus.common.http.server;

public interface HttpRestErrorHandler {
    public Object exception(Throwable e, HttpRequestPkg request);
}
