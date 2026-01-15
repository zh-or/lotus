package or.lotus.core.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UrlMatcher<T> {
    public class Node {
        String path;// {key} => key
        HashMap<String, Node> childs;
        public T obj;

        public Node(String path, Node next, T obj) {
            this.path = path;
            this.childs = new HashMap<>();
            if(next != null) {
                this.childs.put(next.path, next);
            }
            this.obj = obj;
        }

        public void addNext(Node node) {
            this.childs.put(node.path, node);
        }
    }

    Node root;
    ReentrantReadWriteLock lock;

    public UrlMatcher() {
        root = new Node("", null, null);
        lock = new ReentrantReadWriteLock();
    }

    public void clear() {
        try {
            lock.writeLock().lock();
            root.childs.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param url 支持 /a/*, /a/{id}/{page} * 或者 {key} 表示匹配全部
     * */
    public void add(String url, T obj) {
        try {
            lock.writeLock().lock();
            String[] paths = Utils.splitManual(url, "/");
            int len = paths.length;
            if(len == 0) {// `/`
                Node old = root.childs.get("");
                if(old == null) {
                    old = new Node("", null, obj);
                    root.childs.put("", old);
                } else {
                    if(old.obj != null) {
                        throw new RuntimeException(obj.getClass() + " " + url + " 重复定义");
                    }
                    old.obj = obj;
                }
                return;
            }

            Node currentNode = root;
            Map<String, Node> parentMap = null;
            int i = 0;
            do {
                String path = paths[i];
                String key = path;
                parentMap = currentNode.childs;
                if(path.startsWith("{")) {
                    path = path.substring(1, path.length() - 1);
                    key = "*";
                }
                currentNode = parentMap.get(key);
                if(currentNode == null) {
                    currentNode = new Node(path, null, null);
                    parentMap.put(key, currentNode);
                }
                i++;
                if(i >= len) {
                    currentNode.obj = obj;
                    return;
                }
            } while(true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Node findNode(String url) {
        return findNode(url, false);
    }

    public Node findNode(String url, boolean isCheck) {
        String[] paths = Utils.splitManual(url, "/");
        if(paths.length == 0) {
            paths = new String[]{""};
        }
        int len = paths.length;
        Node currentNode = root;
        int i = 0;

        try {
            lock.readLock().lock();
            do {
                String path = paths[i];
                Node tmp = currentNode.childs.get(path);
                if(tmp == null) {
                    if(isCheck) {
                        if(path.startsWith("{")) {
                            currentNode = currentNode.childs.get("*");
                        } else {
                            return null;
                        }
                    } else {
                        currentNode = currentNode.childs.get("*");
                    }
                } else {
                    currentNode = tmp;
                }
                i ++;
                if(i >= len && currentNode != null) {
                    return currentNode;
                }

                if(currentNode == null) {
                    return null;
                }
            } while(true);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 从url的path匹配 */
    public T match(String url) {
        Node node = findNode(url, false);
        if(node != null) {
            return node.obj;
        }
        return null;
    }


}
