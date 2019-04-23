package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import lotus.http.server.HttpMethod;
import lotus.http.server.HttpRequest;
import lotus.http.server.HttpResponse;
import lotus.http.server.HttpServer;
import lotus.http.server.WsRequest;
import lotus.http.server.WsResponse;
import lotus.http.server.support.HttpHandler;
import lotus.http.server.support.WebSocketHandler;
import lotus.nio.Session;

public class Test_http extends HttpHandler{
    static HttpServer httpserver;
    enum Test{
        A(1),
        B(2),
        C(3);
        int type;
        Test(int num){this.type = num;}
    }
    public static void main(String[] args) throws IOException, URISyntaxException {
        URI uri = new URI("ws://a.com/xx?b=1");

        System.out.println(uri.getScheme());
        System.out.println(uri.getHost());
        System.out.println(uri.getPort());
        System.out.println(uri.getPath());
        System.out.println(uri.getQuery());
        
        System.out.println("---------------------------------------");
        
        Test t = Test.valueOf("B");
        System.out.println(t + " " + t.type);
        httpserver = new HttpServer();
        httpserver.addHandler("*", new Test_http());
        httpserver.setKeystoreFilePath("./a.key");
        httpserver.openWebSocket(true);
        httpserver.setWebSocketHandler(new WebSocketHandler() {
            @Override
            public void WebSocketConnection(Session session) {
                System.out.println("ws 连接...");
            }
            @Override
            public void WebSocketMessage(Session session, WsRequest request) {
                System.out.println("ws recv:" + new String(request.getBody()));
                session.write(WsResponse.text("test"));
            }
        });
        httpserver.start(new InetSocketAddress(8090));
        System.out.println("启动完成...");
    }
    
    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) {
//    	response.sendRedirect("/?a=b");
        System.out.println(request.getFullPath());
//        System.out.println(request.getRemoteAddress());
      //  response.setStatus(ResponseStatus.CLIENT_ERROR_NOT_FOUND);
        response.write("hello world");
        
    }
}
