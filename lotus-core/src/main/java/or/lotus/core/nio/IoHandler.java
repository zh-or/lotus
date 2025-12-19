package or.lotus.core.nio;

public abstract class IoHandler {

    /**
     * socket已连接, 但是还未设为异步
     * 此方法为同步方法, 无异常表示确认连接, 抛出异常则丢弃此连接并且不会触发onClose事件
     * @param session
     * @throws Exception
     */
    public void onBeforeConnection(Session session) throws Exception {
    }


    public void onConnection(Session session) throws Exception { }

    public void onReceiveMessage(Session session, Object msg) throws Exception { }

    public void onSentMessage(Session session, Object msg) throws Exception { }

    public void onClose(Session session) throws Exception { }

    /** 空闲事件, 此事件不精确于 setSessionIdleTime 设置的时间, 如需精度更高需调整 NioContext.selectTimeout 的值, 但是调小该值会导致cpu使用率增大 */
    public void onIdle(Session session) throws Exception { }

    public void onException(Session session, Throwable e) {
        e.printStackTrace();
    }
}
