package lotus.socket.common;
import lotus.socket.client.SocketClient;

/**
 * 
 * @author OR
 */
public abstract class ClientCallback {
    public enum EventType{
        ONCLOSE, ONMESSAGERECV, ONSENDT
    }
	/*链接断开时调用*/
    public void onClose(SocketClient sc){}
    /*收到消息时调用*/
    public void onMessageRecv(SocketClient sc, byte[] msg){}
    /*消息发送后调用*/
    public void onSendt(SocketClient sc, byte[] msg){}
    

}
