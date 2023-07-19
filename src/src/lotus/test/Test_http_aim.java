package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import lotus.format.Format;
import lotus.http.server.HttpBaseService;
import lotus.http.server.HttpRestServiceDispatcher;
import lotus.http.server.HttpServer;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.HttpServicePath;
import lotus.log.Log;

@HttpServicePath(path = "/api")
public class Test_http_aim extends HttpBaseService{
    Log log = Log.getLogger();

    public static void main(String[] args) throws IOException {
        Log tLog = Log.getLogger();
        tLog.setLogFileDir("./http-server-test");

        HttpServer server = new HttpServer();
        server.setCharset(Charset.forName("utf-8"));
        server.setEventThreadPoolSize(10);
        server.setCacheBufferSize(1024 * 4);
        HttpRestServiceDispatcher dispatcher = new HttpRestServiceDispatcher();
        dispatcher.addService(new Test_http_aim());
        dispatcher.setErrorHandler((Throwable e, HttpRequest request, HttpResponse response) -> {
            tLog.error("异常: %s", Format.formatException(e));
        });
        server.setHandler(dispatcher);
        server.start(new InetSocketAddress(8888));
    }


    @HttpServicePath(path = "/test")
    public void login(HttpRequest request, HttpResponse response) {
        // 请求路径为 http://host:port/api/test
        String data = request.getBodyString();
        log.info("接收: %s", data);
        response.write("收到登录请求:" + data);
    }
}
