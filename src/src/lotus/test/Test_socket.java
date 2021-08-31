package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import lotus.log.Log;
import lotus.nio.IoHandler;
import lotus.socket.client.AsyncSocketClient;
import lotus.socket.common.ClientCallback;
import lotus.socket.server.SocketServer;
import lotus.utils.Utils;

public class Test_socket {
	static Log log;
	public static void main(String[] args) throws IOException {
		log = Log.getLogger(Test_socket.class);
		log.setProjectName("test");
		SocketServer server = new SocketServer(new InetSocketAddress(5000));
		server.setHandler(new IoHandler() {
                    public void onConnection(lotus.nio.Session session) {
                        log.info("server connection:" + session.getRemoteAddress());
                    };
                    public void onRecvMessage(lotus.nio.Session session, Object msg) {
                        byte[] data = (byte[]) msg;
                        if(data == null){
                            System.out.println("收到了空的消息????");
                            return;
                        }
                        log.info("server event recv msg:" + new String(data) + " addr:" + session.getRemoteAddress());
                        session.write("收到你的消息了".getBytes());
                    };
                    public void onClose(lotus.nio.Session session) {
                        log.warn("server event clientclose:" + session.getRemoteAddress());
                    };
                });
		server.start();
		log.info("server starting");
		AsyncSocketClient client = new AsyncSocketClient(new ClientCallback() {
		    @Override
		    public void onClose(AsyncSocketClient sc) {
		        log.error("client onclose");
		    }
		    @Override
		    public void onMessageRecv(AsyncSocketClient sc, byte[] msg) {
		        log.info("client recv message : %s", new String(msg));
		    }
        });
		client.connection("127.0.0.1", 5000, 5000);
		int i = 0;
        while(true){
            client.send(("msg->" + i++).getBytes());
            client.send(("msg->" + i++).getBytes());
            client.send(("msg->" + i++).getBytes());
          //  client.send(new String(Util.RandomChars(Util.RandomNum(10, 60))).getBytes());
            Utils.SLEEP(5000);
        }
		
	}
}
