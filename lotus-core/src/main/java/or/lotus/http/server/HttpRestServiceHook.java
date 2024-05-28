package or.lotus.http.server;

import io.netty.handler.codec.http.FullHttpResponse;

public abstract class HttpRestServiceHook {
    /**
     * 过滤器
     * @param request
     * @return 返回不为空则拦截请求
     */
    public Object requestHook(HttpRequestPkg request) {
        return null;
   }

    public void responseHook(HttpRequestPkg request, FullHttpResponse response) {

    }
}
