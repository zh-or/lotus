package lotus.nio;

public abstract class IoHandler{

    public void onConnection(Session session) throws Exception{}
    public void onRecvMessage(Session session, Object msg) throws Exception{}
    public void onSentMessage(Session session, Object msg) throws Exception{}
    public void onClose(Session session) throws Exception{}
    public void onIdle(Session session) throws Exception{}
    public void onException(Session session, Throwable e){
    	e.printStackTrace();
    }
}
