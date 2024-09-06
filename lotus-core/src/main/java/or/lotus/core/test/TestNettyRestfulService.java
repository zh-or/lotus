package or.lotus.core.test;

import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.ann.*;
import or.lotus.core.http.netty.NettyHttpServer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@RestfulController("/api")
public class TestNettyRestfulService {

    @Autowired("b1")
    String b1;

    @Get("/hello")
    public String printHello(RestfulRequest request,
                             RestfulResponse response,
                             RestfulContext context,
                             @Parameter("p") String p,
                             @Parameter("arr") String[] arr,
                             @Parameter("int") Integer[] ints,
                             Integer a,
                             @Parameter("list") List<Integer> list) {

        return b1 + ":" + p + ":" + Arrays.toString(arr) + ":" + list.toString();
    }

    @Post("/post")
    public String post(RestfulRequest request, @Parameter("name") String name) {

        return "name:" + name;
    }

    @Post("/postObj")
    public String postObj(RestfulRequest request, @Parameter TestObj obj) {

        return "name:" + obj;
    }

    public class TestObj {
        int a;
        String name;

        public TestObj() {
        }

        @Override
        public String toString() {
            return "TestObj{" +
                    "a=" + a +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException, IOException, ClassNotFoundException {
        NettyHttpServer server = new NettyHttpServer();
        server.addBeans(new TestRestfulBean());
        server.scanController("or.lotus.core.test");
        server.setEventThreadPoolSize(0);
        server.setStaticPath("./test");
        server.start(8000);
    }

}
