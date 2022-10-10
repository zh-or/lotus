package lotus.http.server.support;

public interface HttpRestErrorHandler {
    public void exception(Throwable e, HttpRequest request, HttpResponse response);
}
