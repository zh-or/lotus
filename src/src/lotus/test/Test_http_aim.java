package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import lotus.format.Format;
import lotus.http.server.HttpBaseService;
import lotus.http.server.HttpRestServiceDispatcher;
import lotus.http.server.HttpServer;
import lotus.http.server.support.*;
import lotus.log.Log;

@HttpServicePath(path = "/api")
public class Test_http_aim extends HttpBaseService{
    Log log = Log.getLogger();

    public static void main(String[] args) throws IOException {
        Log tLog = Log.getLogger();
        //tLog.setLogFileDir("./http-server-test");

        HttpServer server = new HttpServer();
        server.setCharset(Charset.forName("utf-8"));
        server.setEventThreadPoolSize(10);
        server.setCacheBufferSize(1024 * 4);
        HttpRestServiceDispatcher dispatcher = new HttpRestServiceDispatcher();
        dispatcher.addService(new Test_http_aim());
        dispatcher.setErrorHandler((Throwable e, HttpRequest request, HttpResponse response) -> {
            tLog.error("异常: %s", Format.formatException(e));
        });
        dispatcher.addServiceFilter(new HttpRestServiceFilter() {

            @Override
            public boolean filter(HttpRequest request, HttpResponse response) {
                response.setHeader("Access-Control-Expose-Headers", "*");
                response.setHeader("Access-Control-Allow-Headers", "*");
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Server", "nginx");//假装是nginx

                if(request.getMethod() == HttpMethod.OPTIONS) {
                    //浏览器的跨域检测
                    return true;
                }
                return false;
            }
        });
        server.setHandler(dispatcher);
        server.start(new InetSocketAddress(8888));
    }


    @HttpServicePath(path = "/test")
    public void login(HttpRequest request, HttpResponse response) {
        // 请求路径为 http://127.0.0.1:8888/api/test
        if(request.getMethod() == HttpMethod.POST) {
            String data = request.getBodyString();
            log.info("接收: %s", data);
            response.write("收到登录请求:" + data);
        } else {

            response.write("收到登录请求: null");
        }
        response.flush();
    }
}
