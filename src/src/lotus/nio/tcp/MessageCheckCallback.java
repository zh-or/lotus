package lotus.nio.tcp;

/**
 * 当使用 writeAndWaitForMessage 时可实现此类来判断返回的消息是否是需要的
 * @author or
 *
 */
public abstract class MessageCheckCallback {

    private Object tmpSendmsg;

    public void setSendMsg(Object msg){
        this.tmpSendmsg = msg;
    }

    public Object getSendMsg(){
        return tmpSendmsg;
    }

    /**
     * @param recvMsg
     * @return 返回 true 则表示此条消息是想要的
     */
    public boolean thatsRight(Object recvMsg){
        return true;
    }
}
