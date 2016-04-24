package lotus.socket.common;
import lotus.socket.client.Client;

/**
 * 
 * @author O_R
 */
public abstract class ClientCallback {
	/*链接断开时调用*/
    public void onClose(Client sc){}
    /*收到消息时调用*/
    public void onMessageRecv(Client sc, byte[] msg){}
    /*消息发送后调用*/
    public void onSendt(Client sc, boolean isok, byte[] msg){}
}
