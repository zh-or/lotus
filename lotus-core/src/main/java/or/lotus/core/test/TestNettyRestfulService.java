package or.lotus.core.test;

import or.lotus.core.http.restful.ann.Autowired;
import or.lotus.core.http.restful.ann.Get;
import or.lotus.core.http.restful.ann.Parameter;
import or.lotus.core.http.restful.ann.RestfulController;
import or.lotus.core.http.restful.netty.NettyRestfulServer;

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
    public String printHello(@Parameter("p") String p,
                             @Parameter("arr") String[] arr) {

        return b1 + ":" + p + ":" + Arrays.toString(arr) + ":" ;
    }

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException, IOException, ClassNotFoundException {
        NettyRestfulServer server = new NettyRestfulServer();
        server.addBeans(new TestRestfulBean());
        server.scanController("or.lotus.core.test");
        server.setEventThreadPoolSize(0);
        server.start(8000);
    }

}
