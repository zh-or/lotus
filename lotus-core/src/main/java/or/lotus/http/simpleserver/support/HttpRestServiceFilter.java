package or.lotus.http.simpleserver.support;

public interface HttpRestServiceFilter {
    /**
     * 过滤器
     * @param request
     * @param response
     * @return 返回true表示拦截请求
     */
    public boolean filter(HttpRequest request, HttpResponse response);
}
