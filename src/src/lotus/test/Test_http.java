package lotus.test;

import java.io.IOException;

import lotus.http.server.HttpHandler;
import lotus.http.server.HttpMethod;
import lotus.http.server.HttpRequest;
import lotus.http.server.HttpResponse;
import lotus.http.server.HttpServer;

public class Test_http extends HttpHandler{
    static HttpServer httpserver;
    enum Test{
        A(1),
        B(2),
        C(3);
        int type;
        Test(int num){this.type = num;}
    }
    public static void main(String[] args) throws IOException {
        
        Test t = Test.valueOf("0");
        System.out.println(t);
/*        httpserver = new HttpServer(0, 1024);
        httpserver.setHandler(new Test_http());
        httpserver.start(new InetSocketAddress(8090));
        System.out.println("启动完成...");*/
    }
    
    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) {
//    	response.sendRedirect("/?a=b");
        System.out.println(request.toString());
//        System.out.println(request.getRemoteAddress());
      //  response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
        response.write("hello world");
        
    }
}
