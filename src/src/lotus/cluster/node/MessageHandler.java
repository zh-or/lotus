package lotus.cluster.node;

import lotus.cluster.Message;

public abstract class MessageHandler {
    public void onRecvMessage(NodeSession node, Message msg){}
    public void onRecvBroadcast(NodeSession node, Message msg){}
    public void onRecvSubscribe(NodeSession node, Message msg){}
    
    public void onClose(NodeSession node){}
}
