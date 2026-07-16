package or.lotus.core.test;

import or.lotus.core.common.DateUtils;
import or.lotus.core.nio.IoHandler;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.support.UdpPackageCodec;
import or.lotus.core.nio.udp.NioUdpClient;
import or.lotus.core.nio.udp.NioUdpServer;
import or.lotus.core.nio.udp.NioUdpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class NioUdpTest {
    public static final Logger log = LoggerFactory.getLogger(NioUdpTest.class);
    static NioUdpServer server;
    static NioUdpClient client;
    static AtomicLong msgCount = new AtomicLong(0);


    public static void main(String[] args) throws IOException {
        server = new NioUdpServer();
        server.setProtocolCodec(new UdpPackageCodec());
        server.setHandler(serverHandler);
        server.bind(62215);
        server.start();

        client = new NioUdpClient();
        client.setProtocolCodec(new UdpPackageCodec());
        client.setHandler(clientHandler);
        client.setSessionIdleTime(5000);
        log.info("开始连接");
        for(int i = 0; i < 1000; i++) {
            long start = System.currentTimeMillis();
            NioUdpSession session = client.connection(new InetSocketAddress("127.0.01", 62215), null);
            long end = System.currentTimeMillis() - start;
            session.write(("--->" + end + "ms").getBytes("utf-8"));
        }
        log.info("1k连接完成");


        Timer tt = new Timer();
        tt.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("消息计数: {}", msgCount.get());
            }
        }, 1000, 1000);
    }

    static IoHandler serverHandler = new IoHandler() {


        @Override
        public void onReceiveMessage(Session session, Object msg) throws Exception {
            session.write(msg);
        }
    };

    static IoHandler clientHandler = new IoHandler() {
        @Override
        public void onReceiveMessage(Session session, Object msg) throws Exception {
            log.info("收到消息: {} => {}", session.getId(), new String((byte[]) msg, "utf-8"));
            msgCount.addAndGet(1);
        }

        @Override
        public void onIdle(Session session) throws Exception {
            session.write(DateUtils.getDateFormat("yyyy-MM-dd HH:mm:ss").getBytes("utf-8"));
        }
    };
}
