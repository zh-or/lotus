package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lotus.log.Log;
import lotus.nio.IoHandler;
import lotus.nio.LineProtocolCodec;
import lotus.nio.Session;
import lotus.nio.tcp.NioTcpServer;


public class Test_nio_server {
    static Log log;
    static AtomicInteger i = new AtomicInteger(0);
    static ConcurrentHashMap<String, Session> c = new ConcurrentHashMap<String, Session>();
    
    public static void main(String[] args) throws IOException {
        log = Log.getInstance();
        log.setProjectName("test");
        NioTcpServer server = new NioTcpServer();
   //     server.setProtocolCodec(new LengthProtocolCode());
        server.setProtocolCodec(new LineProtocolCodec('}'));
        server.setHandler(new IoHandler() {
            @Override
            public void onConnection(Session session) {
          //      log.info("server event connection:" + session.getRemoteAddress());

            }
            @Override
            public void onClose(Session session) {
                log.info("server event close:" + session.getRemoteAddress());
            }
            @Override
            public void onIdle(Session session) {
                c.put(session.toString(), session);
                log.error("server event idle:" + session.getRemoteAddress() + ", count:" + session.getAttr("count") + ", msg size:" + session.getWriteMessageSize() + ", lastmsg:" + session.getAttr("lastmsg"));
            }
            @Override
            public void onRecvMessage(Session session, Object obj) {
                Integer count = (Integer) session.getAttr("count");
                if(count == null) count = new Integer(0);
                String msg = new String((byte[])obj);
                count++;
                session.setAttr("count", count);
                session.setAttr("lastmsg", msg);
                //log.info("server event recv msg->" + session.getRemoteAddress() + "  msg:" + msg);
                i.incrementAndGet();
                session.write(obj);
            }
        });
        server.setSessionIdleTime(60 * 1000);
        server.setSessionCacheBufferSize(1024);
        server.bind(new InetSocketAddress(4000));
        
        
//        NioTcpClient client = new NioTcpClient(new LengthProtocolCode());
    /*    NioTcpClient client = new NioTcpClient(new LineProtocolCodec('}'));
        client.setSessionCacheBufferSize(1024);
        client.setSessionIdleTime(60 * 1000);
        client.init();
        client.setHandler(new IoHandler() {
            @Override
            public void onConnection(Session session) throws Exception {
         //       log.info("client event connectioned..." + session.getRemoteAddress());
                Util.SLEEP(2000);
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
        
        for(int i = 0; i < 59; i++){
            Session isconn = client.connection(new InetSocketAddress("127.0.0.1", 4000), 0);
          //  log.info("wait:" + isconn + isconn.getRemoteAddress());
          //  isconn.write(("fuck -> " + isconn.getId() + "\n").getBytes());
          //  isconn.write(("fuck -> " + isconn.getId() + "}").getBytes());
        }
        System.out.println("连接完毕");
      //  Util.SLEEP(5000);
       // client.close();
        */
        new Timer().schedule(new TimerTask() {
            
            @Override
            public void run() {
                log.info("count:" + i.get() + ", idle:" + c.size());
            }
        }, 1000, 20000);
        
        /*
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                Util.SLEEP(1000);
                Socket socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress("0.0.0.0", 4000));
                    socket.setSoTimeout(2000);
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    Util.SLEEP(1000);
                    byte[] cache = new byte[1024];
                    int i = 0;
                    while(true){
                        out.write(("hello->:" + i).getBytes());
                        out.flush();
                        if(i % 2 == 0){
                        	out.write("\n".getBytes());
                        }
                        try {
                            int len = in.read(cache);
                            if(len > 0){
                                log.info("client recv :" + new String(cache, 0, len));
                                
                            }
                        } catch (Exception e) {
                            // read time out
                        }
                        Util.SLEEP(3000);
                        i++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });*/
    }
}
