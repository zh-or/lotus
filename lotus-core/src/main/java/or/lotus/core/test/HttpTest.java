package or.lotus.core.test;

import or.lotus.core.http.restful.ann.RestfulController;
import or.lotus.core.nio.http.HttpServer;

@RestfulController("/a")
public class HttpTest {
    public static void main(String[] args) {
        HttpServer server = new HttpServer();

    }
}
