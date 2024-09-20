package or.lotus.core.common;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 简易超时hashmap
 * @param <K>
 * @param <V>
 */
public class TimeoutConcurrentHashMap<K, V> extends TimerTask implements AutoCloseable {
    private ConcurrentHashMap<K, ExpireWrap> map;
    private DelayQueue<ExpireWrap<V>> delayQueue;
    private Timer timer;

    private int checkDiff;

    public TimeoutConcurrentHashMap() {
        this(16, 1000);
    }

    public TimeoutConcurrentHashMap(int initialCapacity, int checkDiff) {
        this.checkDiff = checkDiff;
        map = new ConcurrentHashMap<>(initialCapacity);
        delayQueue = new DelayQueue<>();
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
        put(k, v, 0);
    }

    /***
     *
     * @param k
     * @param v
     * @param sec 超时时间秒, 0为不超时
     */
    public void put(K k, V v, int sec) {
       if(sec > 0) {
           ExpireWrap obj = new ExpireWrap(k, v, System.currentTimeMillis() + sec * 1000);
           map.put(k, obj);
           delayQueue.add(obj);
       } else {
           map.put(k, new ExpireWrap(k, v, -1));
       }
    }

    /** 更新对象的过期时间 */
    public void updateTimeout(K k, int sec) {
        ExpireWrap obj = map.get(k);
        if(obj != null) {
            if(sec > 0) {
                obj.expire = System.currentTimeMillis() + sec * 1000;
            } else {
                obj.expire = sec;
            }
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
            delayQueue.remove(val);
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
        ExpireWrap v = null;
        do {
            try {
                v = delayQueue.poll(200, TimeUnit.MILLISECONDS);
                if(v != null) {
                    /** 有可能通过update更新了过期时间, 此处判断一下是否过期, 如果增加了时间需要加回队列 */
                    if(v.isTimeout()) {
                        map.remove(v.k);
                    } else if(v.expire > 0) {
                        delayQueue.add(v);
                    }
                }
            } catch (InterruptedException e) {
                v = null;
            }
        } while(v != null);
        /*for(Map.Entry<K, ExpireWrap> obj : map.entrySet()) {
            if(obj.getValue().isTimeout()) {
                map.remove(obj.getKey());
            }
        }*/
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    private class ExpireWrap <V> implements Delayed {
        K k;
        V obj;
        long expire;

        public ExpireWrap(K k, V obj, long expire) {
            this.k = k;
            this.obj = obj;
            this.expire = expire;
        }

        /**超时返回true, 未超时返回false*/
        public boolean isTimeout() {
            return expire != -1 && System.currentTimeMillis() > expire;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            //延迟任务是否到时就是按照这个方法判断如果返回的是负数则说明到期
            // 否则还没到期
            return unit.convert(expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean equals(Object o) {
            ExpireWrap other = (ExpireWrap) o;
            return this.obj.equals(other.obj);
        }

        @Override
        public int compareTo(Delayed o) {
            ExpireWrap other = (ExpireWrap) o;
            return (int)((expire - System.currentTimeMillis()) - (other.expire - System.currentTimeMillis()));
        }
    }
}
