package or.lotus.core.test;

import or.lotus.core.common.DateUtils;
import or.lotus.core.nio.IoHandler;
import or.lotus.core.nio.Session;
import or.lotus.core.nio.support.LengthProtocolCodec;
import or.lotus.core.nio.tcp.NioTcpClient;
import or.lotus.core.nio.tcp.NioTcpServer;
import or.lotus.core.nio.tcp.NioTcpSession;
import or.lotus.core.socket.SyncSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class SocketTest {
    public static final Logger log = LoggerFactory.getLogger(SocketTest.class);
    static NioTcpServer server;
    static NioTcpClient nioClient;
    static AtomicLong msgCount = new AtomicLong(0);

    public static void main(String[] args) throws IOException {
        int port = 48299;

        server = new NioTcpServer(1024, 5, false);
        server.setExecutor(Executors.newFixedThreadPool(11));
        server.setTcpNoDelay(true);
        server.setHandler(handler);
        server.setProtocolCodec(new LengthProtocolCodec());
        server.setSessionIdleTime(10000);

        server.bind(port);

        server.start();
        log.info("服务已启动: {}", port);
        SyncSocketClient client = new SyncSocketClient();
        client.connection(new InetSocketAddress("127.0.0.1", port), 10000);
        log.info("客户端已连接");
        long t1;
        for(int i = 0; i < 1000; i++) {
            t1 = System.currentTimeMillis();
            client.send(String.valueOf(i).getBytes());
            byte[] data = client.receive();
            if(data != null) {
                log.info("接收一条消息:" + new String(data) + " 耗时:" + (System.currentTimeMillis() - t1) + "ms");
            } else {
                log.error("data is null");
            }
        }
        client.close();

        nioClient = new NioTcpClient(1024, 5, false);
        nioClient.setProtocolCodec(new LengthProtocolCodec());
        nioClient.setHandler(clientHandler);
        nioClient.setSessionIdleTime(5000);

        for(int i = 0; i < 1000; i++) {
            long start = System.currentTimeMillis();
            NioTcpSession session = nioClient.connection(new InetSocketAddress("127.0.0.1", port), 10000);
            long end = System.currentTimeMillis() - start;
            session.write(("--->" + end + "ms").getBytes("utf-8"));
        }
        log.info("1k连接完成");
        //client.close();

        Timer tt = new Timer();
        tt.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("消息计数: {}", msgCount.get());
            }
        }, 1000, 1000);
    }


    static IoHandler handler = new IoHandler() {
        @Override
        public void onConnection(Session session) throws Exception {
            //log.info("session connect: {}", session.getId());
        }

        @Override
        public void onIdle(Session session) throws Exception {
            //log.info("session idle: {}", session.getId());
        }

        public void onReceiveMessage(Session session, Object msg) throws Exception {
            //log.info("session receive: {}, msg: {}", session.getId(), new String((byte[]) msg));

            session.write(msg);
        }

        @Override
        public void onClose(Session session) throws Exception {
            log.info("session close: {}", session.getId());
        }
    };


    static IoHandler clientHandler = new IoHandler() {
        @Override
        public void onConnection(Session session) throws Exception {
            //log.info("clientHandler session connect: {}", session.getId());
        }

        @Override
        public void onIdle(Session session) throws Exception {
            //log.info("clientHandler session idle: {}", session.getId());

            session.write(DateUtils.getDateFormat("yyyy-MM-dd HH:mm:ss").getBytes("utf-8"));
        }

        public void onReceiveMessage(Session session, Object msg) throws Exception {
            //log.info("clientHandler session receive: {}, msg: {}", session.getId(), new String((byte[]) msg));
            msgCount.addAndGet(1);
            //session.write(msg);
        }

        @Override
        public void onClose(Session session) throws Exception {
            //log.info("clientHandler session close: {}", session.getId());
        }
    };
}
