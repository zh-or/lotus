package lotus.cluster.service;

import lotus.cluster.Message;

/**
 * 此监听器的消息并不保证按顺序触发
 * @author O_R
 */
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
    public void onRegSubscribeMessage(ClusterService service, Node node, String action){}
    public void onUnRegSubscribeMessage(ClusterService service, Node node, String action){}
}
