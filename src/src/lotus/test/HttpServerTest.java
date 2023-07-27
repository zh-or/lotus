package lotus.test;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import lotus.http.netty.*;

import java.net.InetSocketAddress;

public class HttpServerTest  extends HttpBaseService {
    public static void main(String[] args) throws Exception {

        HttpServer server = new HttpServer();
        server.addHttpService(new HttpServerTest());
        server.setEventThreadTotal(10);
        server.addServiceHook(new HttpRestServiceHook() {

            @Override
            public void responseHook(HttpRequestPkg request, FullHttpResponse response) {
                HttpHeaders headers = response.headers();
                headers.add(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
                headers.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                headers.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                headers.add(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                headers.add("server", "nginx");
            }
        });
        server.setEnableDirList(true);
        server.start();
        server.bind(new InetSocketAddress(8080));
        System.out.println("服务器启动完成: 8080");

    }

    @HttpServicePath("test")
    public String test(HttpRequestPkg request) {
        System.out.println("收到请求:" + request.getParam("a"));
        return "直接返回字符串";
    }


    @HttpServicePath("jump")
    public HttpResponsePkg jump(HttpRequestPkg request) {
        return HttpResponsePkg.redirect("/test");
    }
}
