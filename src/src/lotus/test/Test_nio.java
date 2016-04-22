package lotus.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import lotus.log.Log;
import lotus.nio.IoHandler;
import lotus.nio.LineProtocolCodec;
import lotus.nio.NioContext;
import lotus.nio.Session;
import lotus.nio.tcp.TcpServer;
import lotus.util.Util;


public class Test_nio {
    static Log log;
    public static void main(String[] args) throws IOException {
        NioContext nio = new TcpServer();
        log = Log.getInstance();
        log.setProjectName("test");
        nio.setProtocolCodec(new LineProtocolCodec());
        
        nio.setHandler(new IoHandler() {
            @Override
            public void onConnection(Session session) {
                log.info("server event connection:" + session.getRemoteAddress());
            }
            @Override
            public void onClose(Session session) {
                log.info("server event close:" + session.getRemoteAddress());
            }
            @Override
            public void onIdle(Session session) {
                log.info("server event idle:" + session.getRemoteAddress());
            }
            @Override
            public void onRecvMessage(Session session, Object obj) {
                String msg = new String((byte[])obj);
                Util.SLEEP(10000);
                log.info("server event recv msg->" + session.getRemoteAddress() + "  msg:" + msg);
                session.write(obj);
            }
        });
        nio.setSessionIdleTime(60 * 1000);
        nio.setSessionReadBufferSize(5);
        nio.bind(new InetSocketAddress(4000));

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
                        out.write(("hello world! - > " + i).getBytes());
                        out.flush();
                        if(i % 2 == 0){
                        	out.write('\n');
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
        }).start();
    }
}
