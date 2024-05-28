package or.lotus.common.http.server;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.io.File;

public abstract class HttpStaticFileHook {
    /**
     * 过滤器
     *
     * @param request
     * @param file
     * @return 返回不为空则拦截请求
     */
    public Object requestHook(HttpRequest request, File file) {
        return null;
   }

    public void responseHook(HttpRequest request, HttpResponse response) {

    }
}
