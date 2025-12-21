package or.lotus.core.test;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.Get;
import or.lotus.core.http.restful.ann.RestfulController;
import or.lotus.core.nio.LotusByteBuffer;
import or.lotus.core.nio.http.HttpResponse;
import or.lotus.core.nio.http.HttpServer;
import or.lotus.core.nio.http.HttpSyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

@RestfulController("/a")
public class HttpTest {
    private static final Logger log = LoggerFactory.getLogger(HttpTest.class);

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer();
        byte[] b = new byte[127];
        for(byte i = 0; i < b.length; i ++) {
            b[i] = (byte)i;
        }

        LotusByteBuffer buf = (LotusByteBuffer) server.getNioTcpServer().pulledByteBuffer();
        buf.append(b);
        buf.flip();
        int p1 = buf.search(new byte[]{50, 51, 52});
        int p2 = buf.search(new byte[]{90, 91});
        int p3 = buf.search(new byte[]{90, 92});

        server.addController(HttpTest.class);
        server.start(9999);
        log.info("启动完成: 9999");
    }


    @Get("/haha")
    public String haha() {
        return "test-haha";
    }

    @Get("/sync")
    public void sync(HttpResponse response) {
        HttpSyncResponse sync = response.openSync();

        new Thread(() -> {
            for(int i = 0; i < 10; i++) {
                sync.write("sync-" + i + "\n");
                sync.flush();
                Utils.SLEEP(3000);
            }
            sync.syncEnd();
        }).start();
    }
}
