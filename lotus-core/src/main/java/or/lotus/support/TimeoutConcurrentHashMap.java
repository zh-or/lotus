package or.lotus.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易超时hashmap
 * @param <K>
 * @param <V>
 */
public class TimeoutConcurrentHashMap<K, V> extends TimerTask implements Closeable {
    private ConcurrentHashMap<K, ExpireWrap> map;
    private Timer timer;

    private int checkDiff;

    public TimeoutConcurrentHashMap() {
        this(0, 1000);
    }

    public TimeoutConcurrentHashMap(int initialCapacity, int checkDiff) {
        this.checkDiff = checkDiff;
        map = new ConcurrentHashMap<>(initialCapacity);
        timer = new Timer();
        timer.schedule(this, checkDiff, checkDiff);
    }

    public int getCheckDiff() {
        return checkDiff;
    }

    /***
     * 此方法加入的对象将永远不会超时
     * @param k
     * @param v
     */
    public void put(K k, V v) {
        put(k, v, -1);
    }

    /***
     *
     * @param k
     * @param v
     * @param sec 超时时间
     */
    public void put(K k, V v, int sec) {
       if(sec > 0) {
           map.put(k, new ExpireWrap(v, System.currentTimeMillis() + sec * 1000));
       } else {
           map.put(k, new ExpireWrap(v, -1));
       }
    }

    public Enumeration<K> keys() {
        return map.keys();
    }

    public boolean has(K k) {
        ExpireWrap v = map.get(k);
        return v != null && !v.isTimeout();
    }

    public V remove(K k) {
        ExpireWrap val = map.remove(k);
        if(val != null) {
            return (V) val.obj;
        }
        return null;
    }

    public V get(K k) {
        ExpireWrap v = map.get(k);
        if(v != null) {
            if(v.isTimeout()) {
                map.remove(k);
                return null;
            }
            return (V) v.obj;
        }
        return null;
    }

    public void shutdown() {
        if(timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void run() {
        for(Map.Entry<K, ExpireWrap> obj : map.entrySet()) {
            if(obj.getValue().isTimeout()) {
                map.remove(obj.getKey());
            }
        }
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    private class ExpireWrap <V> {
        V obj;
        long expire;

        public ExpireWrap(V obj, long expire) {
            this.obj = obj;
            this.expire = expire;
        }

        public boolean isTimeout() {
            return expire != -1 && System.currentTimeMillis() > expire;
        }
    }
}
