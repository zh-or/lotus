package lotus.test;

import lotus.http.HttpServer;

import java.net.InetSocketAddress;

public class HttpServerTest {

    public static void main(String[] args) throws InterruptedException {
        HttpServer server = new HttpServer();
        server.start();
        server.bind(new InetSocketAddress(8080));
    }
}
