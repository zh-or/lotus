package lotus.test;

import java.io.IOException;

import lotus.log.Log;
import lotus.nio.IoHandler;
import lotus.socket.client.Client;
import lotus.socket.common.ClientCallback;
import lotus.socket.server.SocketServer;
import lotus.util.Util;

public class Test_socket {
	static Log log;
	public static void main(String[] args) throws IOException {
		log = Log.getInstance();
		log.setProjectName("test");
		SocketServer server = new SocketServer("0.0.0.0", 5000, 5, 1024, 3 * 60, 100, 20 * 1000, 
				new IoHandler() {
					public void onConnection(lotus.nio.Session session) {
						log.info("connection:" + session.getRemoteAddress());
					};
					public void onRecvMessage(lotus.nio.Session session, Object msg) {
						byte[] data = (byte[]) msg;
						if(data == null){
						    System.out.println("收到了空的消息????");
						    return;
						}
						log.info("recv msg:" + new String(data) + " addr:" + session.getRemoteAddress());
						SocketServer.send(session, "收到你的消息了".getBytes());
					};
					public void onClose(lotus.nio.Session session) {
						log.warn("close:" + session.getRemoteAddress());
					};
				}
		);
		
		server.start();
		Client client = new Client("0.0.0.0", 5000, 20 * 1000, 
                new ClientCallback() {
                    public void onClose(Client sc) {
                        
                    };
                    public void onMessageRecv(Client sc, byte[] msg) {
                        log.debug("服务器回复消息:" + new String(msg));
                    };
                }
        );
        client.connection();
        
        while(true){

            client.send("123".getBytes());
            client.send("123".getBytes());
            client.send("123".getBytes());
          //  client.send(new String(Util.RandomChars(Util.RandomNum(10, 60))).getBytes());
            Util.SLEEP(2000);
        }
		
	}
}
