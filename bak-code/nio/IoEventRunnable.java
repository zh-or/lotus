package or.lotus.core.nio;


import or.lotus.core.nio.tcp.NioTcpSession;

/**
 * 保证同一个session的事件的顺序执行
 * @author or
 */
public class IoEventRunnable implements Runnable{
    public enum IoEventType{
        SESSION_CONNECTION,
        SESSION_CLOSE,
        SESSION_RECVMSG,
        SESSION_SENT,
        SESSION_IDLE,
        SESSION_EXCEPTION
    }

    private Object att;
    private IoEventType type;
    private Session session;
    private NioContext context;
//    private SeqExecutorService seqexservice;

    public IoEventRunnable(Object att, IoEventType type, Session session, NioContext context) {
        this.att = att;
        this.type = type;
        this.session = session;
        this.context = context;
    }

    @Override
    public void run() {
//        long s = System.currentTimeMillis();
        Throwable _e = null;
        try {
            IoHandler handler = session.getEventHandler();
            switch (type) {
                case SESSION_CONNECTION:
                    handler.onConnection(session);
                    break;
                case SESSION_IDLE:
                    handler.onIdle(session);
                    session.setLastActive(System.currentTimeMillis());
                    break;
                case SESSION_RECVMSG:
                    if(session.isWaitForRecvPack() && session instanceof NioTcpSession && ((NioTcpSession) session).callCheckMessageCallback(att)){
                        session.setPack(att);
                        //session.packNotifyAll();
                    }else{
                        handler.onRecvMessage(session, att);
                    }
                    break;
                case SESSION_SENT:
                    handler.onSentMessage(session, att);
                    break;
                case SESSION_CLOSE:
                    handler.onClose(session);
                    break;
                case SESSION_EXCEPTION:
                    try{
                        //这里的错误必须处理 否则会递归死循环
                        handler.onException(session, (Throwable) att);
                    } catch(Exception tobe) {
                        tobe.printStackTrace();
                    }
                    break;
            }
            IoEventRunnable iorun = null;
            while((iorun = (IoEventRunnable) session.pullEventRunnable()) != null){
                iorun.run();
            }
        } catch (Throwable e) {
            _e = e;
        }
        if(_e != null){
            session.pushEventRunnable(new IoEventRunnable(_e, IoEventType.SESSION_EXCEPTION, session, context));
        }
//        System.out.println("事件处理用时->" + (System.currentTimeMillis() - s) + " time:" + System.currentTimeMillis());
    }
}
