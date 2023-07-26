package lotus.http.netty;

public interface HttpRestErrorHandler {
    public Object exception(Throwable e, HttpRequestPkg request);
}
