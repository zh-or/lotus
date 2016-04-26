package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.http.server.HttpHandler;
import lotus.http.server.HttpMethod;
import lotus.http.server.HttpServer;
import lotus.http.server.Request;
import lotus.http.server.Response;

public class Test_http extends HttpHandler{
    static HttpServer httpserver;
    
    public static void main(String[] args) throws IOException {
        httpserver = new HttpServer();
        httpserver.setHandler(new Test_http());
        httpserver.start(new InetSocketAddress(8090));
        System.out.println("启动完成...");
    }
    
    @Override
    public void service(HttpMethod mothed, Request request, Response response) {
        System.out.println(request.toString());
    }
}
