package lotus.cluster.service;

import lotus.cluster.Message;

public abstract class MessageListenner {
    public void onNodeInit(ClusterService service, Node node){}
    public void onNodeUnInit(ClusterService service, Node node){}
    public void onNodeConnectionsChanged(ClusterService service, Node node){}
    /**
     * @param node
     * @param msg
     * @return 返回 true 表示拦截此广播
     */
    public boolean onRecvBroadcast(ClusterService service, Node node, Message msg){return false;}
    public void onRecvSubscribe(ClusterService service, Node node, Message msg){}
    
    public void onRecvMessage(ClusterService service, Message msg){}
    public void onMessageSent(ClusterService service, Object obj){}
    public void onSubscribeMessage(ClusterService service, Node node, String action){}
    public void onUnSubscribeMessage(ClusterService service, Node node, String action){}
}
