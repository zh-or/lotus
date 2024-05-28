package or.lotus.common.http.simpleserver.support;

public interface HttpFileFilter {
    /**
     * 文件过滤器
     * @param request
     * @param response
     * @return 返回true表示拦截请求
     */
    public boolean filter(String path, HttpRequest request, HttpResponse response);
}
