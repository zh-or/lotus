package or.lotus.core.nio.support;

import or.lotus.core.nio.support.NioSession;

public abstract class NioServerHandler<T> {

    /**
     * socket已连接, 但是还未设为异步, 此方法为同步方法返回true表示确认连接, 返回false则丢弃此连接并且不会触发onClose事件
     * @param session
     * @return
     * @throws Exception
     */
    public boolean onBeforeConnection(NioSession session) throws Exception {
        return true;
    };

    public void onConnection(NioSession session) throws Exception{}

    public void onMessage(NioSession session, T msg) throws Exception{}

    public void onSentMessage(NioSession session, T msg) throws Exception{}

    public void onClose(NioSession session) throws Exception{}

    public void onIdle(NioSession session) throws Exception{}

    public void onException(NioSession session, Throwable e) {
    	e.printStackTrace();
    }
}
