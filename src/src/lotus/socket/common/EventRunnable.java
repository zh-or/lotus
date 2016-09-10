package lotus.socket.common;

import lotus.socket.client.AsyncSocketClient;
import lotus.socket.common.ClientCallback.EventType;

public class EventRunnable implements Runnable{
    private EventType type;
    private ClientCallback callback;
    private AsyncSocketClient socket;
    private Object att;
    
    public EventRunnable(EventType type, ClientCallback callback, AsyncSocketClient socket, Object att) {
       this.type = type;
       this.callback = callback;
       this.socket = socket;
       this.att = att;
    }

    @Override
    public void run() {
        switch (type) {
            case ONMESSAGERECV:
                callback.onMessageRecv(socket, (byte[])att);
                break;
            case ONSENDT:
                callback.onSendt(socket, (byte[]) att);
                break;
            case ONCLOSE:
                callback.onClose(socket);
                break;
        }
    }
    
}
