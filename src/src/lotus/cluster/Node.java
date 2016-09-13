package lotus.cluster;

import java.util.ArrayList;

import lotus.nio.Session;


public class Node {

    private Session                 session_cmd     = null;
    private Session[]               session_data    = null;
    private ArrayList<String>       subs            = null;
    private String                  nodeid          = "";
    private int                     limit           = -1;
    private int                     capacity        = 0;
    private int                     usebound        = 0;
    
    public Node(Session session_cmd, String nodeid, int connmax) {
        this.session_cmd = session_cmd;
        this.session_data = new Session[connmax];
        this.subs = new ArrayList<String>();
        this.nodeid = nodeid;
        this.capacity = connmax;
    }
    
    public void reSetCmdSession(Session session_cmd){
        if(this.session_cmd != null){
            this.session_cmd.closeNow();
            this.session_cmd = null;
        }
        this.session_cmd = session_cmd;
    }
    
    public Session getCmdSession(){
        return session_cmd;
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
    
    /**
     * 当前已有连接数
     * @return 返回 -1 则表示无连接, 否则返回连接数量
     */
    public int limit(){
        return limit == -1 ? -1 : limit + 1;
    }
    
    public int capacity(){
        return capacity;
    }
    
    /**
     * 如果当前容量已满则会忽略本次添加的连接
     * @param conn
     */
    public synchronized void AddDataSession(Session conn){
        if(capacity > limit){
            limit++;
            session_data[limit] = conn;
        }
    }
    
    public synchronized void removeDataSession(Session conn){
        for(int i = 0; i <= limit; i++){
             if(session_data[i] == conn){
                 System.arraycopy(session_data, i + 1, session_data, i, limit - i);
                 limit--;
                 return;
             }
        }
    }
    
    public synchronized Session getOneConnection(){
        Session conn = null;
        if(limit > 0){
            conn = session_data[usebound];
            usebound++;
            if(usebound > limit) usebound = 0;
        }
        return conn;
    }
    
}
