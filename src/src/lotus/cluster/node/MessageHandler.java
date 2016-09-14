package lotus.cluster.node;

import lotus.cluster.Message;
import lotus.cluster.MessageResult;

public abstract class MessageHandler {
    public void onRecvMessage(NodeSession node, Message msg){}
    public void onRecvMessageResponse(NodeSession node, MessageResult msgres){}
    public void onRecvBroadcast(NodeSession node, Message msg){}
    public void onRecvSubscribe(NodeSession node, String action, Message msg){}
    
/*    public void onClose(NodeSession node){}*/
}
