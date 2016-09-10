package lotus.cluster;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import lotus.nio.Session;


public class Node {

    private Session                 session_cmd     = null;
    private ArrayList<Session>      session_data    = null;
    private ArrayList<String>       subs            = null;
    private String                  nodeid          = "";
    private AtomicInteger           limit           = null;
    private AtomicInteger           capacity        = null;
    
    public Node(Session session_cmd, String nodeid) {
        this.session_cmd = session_cmd;
        this.session_data = new ArrayList<Session>();
        this.subs = new ArrayList<String>();
        this.nodeid = nodeid;
        this.limit = new AtomicInteger(0);
        this.capacity = new AtomicInteger(0);
        
    }
    
    public Session getCmdSession(){
        return session_cmd;
    }
    
    public Session getOneDataSession(){
        if(capacity.get() > 0){
            
        }
        return null;
    }
    
    public String getNodeId(){
        return nodeid;
    }
    
    public synchronized void addSubs(String type){
        if(subs.contains(type) == false){
            subs.add(type);
        }
    }
    
    public synchronized void removeSubs(String type){
        subs.remove(type);
    }
    
    public int limit(){
        return limit;
    }
    
    public void limit(int l){
        this.limit = l;
    }
    
    public int capacity(){
        return capacity;
    }
    
    public void capacity(int c){
        this.capacity = c;
    }
    
    public Session getOneConnection(){
        Session conn = null;
        if(capacity > 0){
            
        }
        return conn;
    }
    
}
