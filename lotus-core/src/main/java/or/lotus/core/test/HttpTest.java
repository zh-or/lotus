package or.lotus.core.test;

import or.lotus.core.common.Utils;
import or.lotus.core.files.FileSize;
import or.lotus.core.http.WebSocketFrame;
import or.lotus.core.http.restful.RestfulFilter;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.ann.Get;
import or.lotus.core.http.restful.ann.Parameter;
import or.lotus.core.http.restful.ann.Post;
import or.lotus.core.http.restful.ann.RestfulController;
import or.lotus.core.nio.LotusByteBuffer;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

@RestfulController("/a")
public class HttpTest {
    private static final Logger log = LoggerFactory.getLogger(HttpTest.class);

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer();
        byte[] b = new byte[50];
        for(byte i = 0; i < b.length; i ++) {
            b[i] = (byte)i;
        }
        byte[] b2 = new byte[127];
        for(byte i = 0; i < b.length; i ++) {
            b2[i] = (byte)(i + 50);
        }

        LotusByteBuffer buf = (LotusByteBuffer) server.getNioTcpServer().pulledByteBuffer();
        buf.append(ByteBuffer.wrap(b));
        buf.append(ByteBuffer.wrap(b2));
        //buf.flip();
        int p0 = buf.search(new byte[]{10, 11, 12});
        int p1 = buf.search(new byte[]{50, 51, 52});
        int p2 = buf.search(new byte[]{90, 91});
        int p3 = buf.search(new byte[]{90, 92});

        log.info("search p0:{}, p1:{}, p2:{}, p3:{}", p0, p1, p2, p3);

        server.addStaticPath("./test");
        server.addController(HttpTest.class);
        server.setCacheContentToFileLimit(1024 * 5);
        server.setFilter(
                new RestfulFilter() {
                    @Override
                    public boolean exception(Throwable e, RestfulRequest request, RestfulResponse response) {
                        log.error("发生错误:", e);
                        return false;
                    }
                }
        );
        server.addWebSocketMessageHandler(wsHandler);
        server.setEventThreadPoolSize(5);
        server.start(9999);
        log.info("启动完成: 9999");

        Timer t = new Timer();

        t.schedule(new TimerTask() {
            long last = 0;
            @Override
            public void run() {
                long useMem = server.getNioTcpServer().getFlyByByteBufferCount();

                if(useMem != last) {
                    System.out.println("当前使用中的内存:" + new FileSize(useMem));
                    last = useMem;
                }
            }
        }, 1000, 500);
    }


    @Get("/haha")
    public String haha(@Parameter("t") String t) {
        return "test-haha:->" + t;
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

    @Post("/upload")
    public String post(HttpRequest request, @Parameter("a") String aa) {
        log.info("content length: {}", request.getContentLength());
        if(request.isMultipart()) {
            HttpBodyData formData = request.getBodyFormData();

            File file = formData.getFormDataItemFile("b");

            log.info("upload: {}, {} => {}", formData.getFormDataItemValue("a") + "=" + aa,
                    file.getAbsolutePath(), file.length());

        }

        return "post-" + 1;
    }


    @Post("/par")
    public String postPar(@Parameter("par") String par) {
        return "par-"  + par;
    }

    @Get("/f")
    public File f(@Parameter("par") String par) {
        return new File("./orm.md");
    }

    static HttpWebSocketMessageHandler wsHandler = new HttpWebSocketMessageHandler("/ws") {
        @Override
        public void onConnection(HttpRequest request, Session session) throws Exception {
            log.info("ws onConnection: {}", session.getId());
        }

        @Override
        public void onClose(Session session) throws Exception {
            log.info("ws onClose: {}", session.getId());
        }


        @Override
        public void onIdle(Session session) throws Exception {
            log.info("ws onIdle: {}", session.getId());
        }

        @Override
        public void onMessage(Session session, WebSocketFrame msg) throws Exception {
            log.info("ws onMessage: {}, {}", session.getId(), msg);
            if(msg.opcode == WebSocketFrame.OPCODE_PING) {
                session.write(WebSocketFrame.pong());
                return;
            }
            int len = Integer.valueOf(msg.getText());
            log.info("收到数字: {}", len);
            StringBuilder sb = new StringBuilder(len);
            for(int i = 0; i < len; i++) {
                sb.append("1");
            }

            session.write(WebSocketFrame.text(sb.toString()));
        }

        @Override
        public void onException(Session session, Throwable e) {
            log.info("ws onException: {}", session.getId(), e);
        }
    };
}
