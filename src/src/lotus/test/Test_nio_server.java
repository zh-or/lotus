package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import lotus.log.Log;
import lotus.nio.IoHandler;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpServer;
import lotus.socket.client.SyncSocketClient;
import lotus.socket.common.LengthProtocolCode;
import lotus.util.Util;


public class Test_nio_server {
    static Log log;
    static AtomicInteger i = new AtomicInteger(0);
    
    public static void main(String[] args) throws IOException {
        log = Log.getInstance();
        log.setProjectName("test");
        NioTcpServer server = new NioTcpServer();
        server.setProtocolCodec(new LengthProtocolCode());
     //   server.setProtocolCodec(new LineProtocolCodec('}'));
        server.setHandler(new IoHandler() {
            @Override
            public void onConnection(Session session) {

            }
            @Override
            public void onClose(Session session) {
                log.info("客户端断开了:" + session.getRemoteAddress());
            }
            
            @Override
            public void onRecvMessage(Session session, Object obj) {
                i.incrementAndGet();
                byte[] data = (byte[]) obj;
                log.info("收到数据长度:" + data.length);
                session.write("ok".getBytes());
            }
        });
        server.setSessionCacheBufferSize(1024);
        server.bind(new InetSocketAddress(4000));
        
        SyncSocketClient client = new SyncSocketClient();
        client.setRecvTimeOut(1000 * 20);
        if(!client.connection(new InetSocketAddress("127.0.0.1", 4000), 10000)){
            log.error("连接到服务器出错.");
            System.exit(1);
        }
        log.info("启动完成...");
        Util.SLEEP(1000);
        log.info("发送 1021...");
        log.info("收到:%s", Util.byte2hex(client.send(Util.GetRepeatByte(1021, (byte)56))));
        log.info("发送 255...");
        log.info("收到1:%s", Util.byte2hex(client.send(Util.GetRepeatByte(255, (byte)56))));
        log.info("发送 1024...");
        log.info("收到1:%s", Util.byte2hex(client.send(Util.GetRepeatByte(1024, (byte)56))));
        
        /*
        Util.SLEEP(1000);
        String tmp = new String(Util.RandomChars(800));
        log.info("收到1:%s", Util.byte2hex(client.send(tmp.getBytes())));
        
        Util.SLEEP(1000);
        tmp = new String(Util.RandomChars(255));
        log.info("收到2:%s", Util.byte2hex(client.send(tmp.getBytes())));
        
        Util.SLEEP(1000);
        tmp = new String(Util.RandomChars(1056));
        log.info("收到3:%s", Util.byte2hex(client.send(tmp.getBytes())));
        */
    }
}
