package lotus.cluster;

import java.util.ArrayList;

import lotus.nio.Session;

public class Node {
    private Session session;
    private ArrayList<String> subscribeActions;
    private String nodeid;
    
    public Node(Session session, String nodeid) {
        this.session = session;
        this.subscribeActions = new ArrayList<String>();
        this.nodeid = nodeid;
    }
    
    public synchronized void addSubscribe(String action){
        if(subscribeActions.contains(action)){
           return; 
        }
        subscribeActions.add(action);
    }
    
    public synchronized void removeSubscribe(String action){
        subscribeActions.remove(action);
    }
    
    public synchronized void updateSession(Session session){
        this.session = session;
    }
    
    public Session getSession(){
        return this.session;
    }
    
    public String getNodeId(){
        return this.nodeid;
    }
    
    public synchronized ArrayList<String> getSubscribeActions(){
        @SuppressWarnings("unchecked")
        ArrayList<String> tmp = (ArrayList<String>) subscribeActions.clone();
        return tmp;
    }
}
