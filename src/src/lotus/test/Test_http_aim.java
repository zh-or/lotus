package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import lotus.http.server.HttpBaseService;
import lotus.http.server.HttpRestServiceDispatcher;
import lotus.http.server.HttpServer;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.HttpServicePath;

@HttpServicePath(path = "/test")
public class Test_http_aim extends HttpBaseService{

    public static void main(String[] args) throws IOException {
        HttpServer server = new HttpServer();
        server.setCharset(Charset.forName("utf-8"));
        server.setEventThreadPoolSize(10);
        server.setReadBufferCacheSize(1024 * 4);
        HttpRestServiceDispatcher dispathcer = new HttpRestServiceDispatcher();
        dispathcer.addService(new Test_http_aim());
        server.setHandler(dispathcer);
        server.start(new InetSocketAddress(8888));
    }


    @HttpServicePath(path = "/login")
    public void login(HttpRequest request, HttpResponse response) {
        // 请求路径为 http://host:port/user/login
        System.out.println("登录调用");
        response.write("收到登录请求");
    }
}
