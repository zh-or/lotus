package lotus.nio;

public abstract class IoHandler {
    
    /**
     * socket已连接, 但是还未设为异步, 此方法为同步方法返回true表示确认连接, 返回false则丢弃此连接并且不会触发onClose事件
     * @param session
     * @return
     * @throws Exception
     */
    public boolean onBeforeConnection(Session session) throws Exception {
        return true;
    };
    public void onConnection(Session session) throws Exception{}
    public void onRecvMessage(Session session, Object msg) throws Exception{}
    public void onSentMessage(Session session, Object msg) throws Exception{}
    public void onClose(Session session) throws Exception{}
    public void onIdle(Session session) throws Exception{}
    public void onException(Session session, Throwable e){
    	e.printStackTrace();
    }
}
