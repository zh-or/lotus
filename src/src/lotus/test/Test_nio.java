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
import lotus.nio.tcp.NioTcpClient;
import lotus.nio.tcp.NioTcpServer;
import lotus.util.Util;


public class Test_nio {
    static Log log;
    public static void main(String[] args) throws IOException {
        log = Log.getInstance();
        log.setProjectName("test");
        NioContext server = new NioTcpServer();
        server.setProtocolCodec(new LineProtocolCodec());
        
        server.setHandler(new IoHandler() {
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
                log.info("server event recv msg->" + session.getRemoteAddress() + "  msg:" + msg);
                session.write(obj);
            }
        });
        server.setSessionIdleTime(60 * 1000);
        server.setSessionReadBufferSize(5);
        server.bind(new InetSocketAddress(4000));
        
        
        NioTcpClient client = new NioTcpClient(new LineProtocolCodec());
        client.setSessionReadBufferSize(5);
        client.setHandler(new IoHandler() {
            @Override
            public void onConnection(Session session) throws Exception {
                log.info("client event connectioned...");
                Util.SLEEP(2000);

                session.write(("fuck -> " + session.getId() + "\n").getBytes());
            }
            @Override
            public void onRecvMessage(Session session, Object msg) throws Exception {
                log.info("client recv:%s", new String((byte[])msg));

                session.write(("fuck -> " + session.getId() + "\n").getBytes());
            }
        });
        
        for(int i = 0; i < 100; i++){
            boolean isconn = client.connection(new InetSocketAddress("127.0.0.1", 4000), 3000);
            log.info("wait:" + isconn);
            
        }
        
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
        });
    }
}
