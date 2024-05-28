package or.lotus.common.http.simpleserver.support;

public interface HttpRestErrorHandler {
    public void exception(Throwable e, HttpRequest request, HttpResponse response);
}
