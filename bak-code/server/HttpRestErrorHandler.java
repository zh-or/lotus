package or.lotus.core.http.server;

public interface HttpRestErrorHandler {
    public Object exception(Throwable e, HttpRequestPkg request);
}
