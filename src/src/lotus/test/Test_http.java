package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.http.server.HttpHandler;
import lotus.http.server.HttpMethod;
import lotus.http.server.HttpServer;
import lotus.http.server.HttpRequest;
import lotus.http.server.HttpResponse;

public class Test_http extends HttpHandler{
    static HttpServer httpserver;
    
    public static void main(String[] args) throws IOException {
        httpserver = new HttpServer(1, 10, 1024);
        httpserver.setHandler(new Test_http());
        httpserver.start(new InetSocketAddress(8090));
        System.out.println("启动完成...");
    }
    
    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) {
//    	response.sendRedirect("/?a=b");
//        System.out.println(request.toString());
//        
        response.write("hello world");
        
    }
}
