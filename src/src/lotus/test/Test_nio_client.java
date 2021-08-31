package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

import lotus.log.Log;
import lotus.nio.IoHandler;
import lotus.nio.LineProtocolCodec;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpClient;
import lotus.utils.Utils;

public class Test_nio_client {
    static Log log = null;
    public static void main(String[] args) throws IOException {
        log = Log.getLogger();
        log.setProjectName("test");
        NioTcpClient client = new NioTcpClient();
        client.setProtocolCodec(new LineProtocolCodec('}'));
        client.setSessionCacheBufferSize(1024);
        client.setSessionIdleTime(60 * 1000);
        client.init();
        client.setHandler(new IoHandler() {
            @Override
            public void onConnection(Session session) throws Exception {
         //       log.info("client event connectioned..." + session.getRemoteAddress());
                Utils.SLEEP(2000);
                session.setAttr("msgcount", 1);
           //     session.write(("fuck -> " + session.getId() + "\n").getBytes());
                session.write(("1}").getBytes());
            }
            @Override
            public void onRecvMessage(Session session, Object msg) throws Exception {
             //   log.info("client recv:%s", new String((byte[])msg));

            //    session.write(("fuck -> " + session.getId() + "\n").getBytes());
                int msgcount = (Integer) session.getAttr("msgcount");
                msgcount ++;
                session.setAttr("msgcount", msgcount);
                String lastmsg = "fuck -> " + msgcount + "}";
                session.setAttr("lastmsg", lastmsg);
                session.write(lastmsg.getBytes());
            }
            @Override
            public void onIdle(Session session) throws Exception {
                // TODO Auto-generated method stub
                log.error("client event idle session:" + session.getLocaAddress() + ", msg size:" + session.getWriteMessageSize() + ", lastmsg:" + session.getAttr("lastmsg"));
                super.onIdle(session);
            }
            @Override
            public void onClose(Session session) throws Exception {
                log.info("client event close");
            }
        });
        System.out.print("请输入连接数:");
        @SuppressWarnings("resource")
        Scanner in = new Scanner(System.in);
        int num = in.nextInt();
        System.out.println("连接数:" + num);
        for(int i = 0; i < num; i++){
            Session isconn = client.connection(new InetSocketAddress("192.168.0.140", 4000), 0);
            isconn.getId();
          //  log.info("wait:" + isconn + isconn.getRemoteAddress());
          //  isconn.write(("fuck -> " + isconn.getId() + "\n").getBytes());
          //  isconn.write(("fuck -> " + isconn.getId() + "}").getBytes());
            Utils.SLEEP(100);
        }
        System.out.println("连接完毕");
    }
}
