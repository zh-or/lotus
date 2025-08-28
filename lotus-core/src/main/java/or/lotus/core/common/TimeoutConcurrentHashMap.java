package or.lotus.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 超时hashmap
 * @param <K>
 * @param <V>
 */
public class TimeoutConcurrentHashMap<K, V> implements AutoCloseable, Runnable {
    static Logger log = LoggerFactory.getLogger(TimeoutConcurrentHashMap.class);
    private ConcurrentHashMap<K, ExpireWrap> map;
    private DelayQueue<ExpireWrap<V>> delayQueue;
    private String name;
    private TimeoutListener listener = null;
    private Thread runner;
    private boolean isRun = true;
    public static long maxItems = 100_000_000;//最大存储数量, 默认1亿

    public TimeoutConcurrentHashMap() {
        this("-");
    }

    public TimeoutConcurrentHashMap(String name) {
        this(16, name);
    }

    public TimeoutConcurrentHashMap(int initialCapacity, String name) {
        this.name = name;
        map = new ConcurrentHashMap<>(initialCapacity);
        delayQueue = new DelayQueue<>();
        runner = new Thread(this, "timeout checker[" + name + "]");
        runner.start();
    }

    public void setListener(TimeoutListener<K, V> listener) {
        this.listener = listener;
    }

    public void clear() {
        map.clear();
        delayQueue.clear();
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
        ExpireWrap obj = map.get(k);
        long timeout = sec > 0 ? System.currentTimeMillis() + sec * 1000 : -1;
        if (obj == null) {
            obj = new ExpireWrap(k, v, timeout);
        } else {
            synchronized (obj) {
                obj.obj = v;
                obj.expire = timeout;
            }
        }
        if (sec > 0) {
            map.put(k, obj);
            delayQueue.add(obj);
        } else {
            map.put(k, obj);
        }
        if (map.size() >= maxItems) {
            log.warn("TimeoutConcurrentHashMap [{}] 元素已达警告数量 {} >= {}", name, map.size(), maxItems);
        }

    }

    /** 更新对象的过期时间 */
    public void updateTimeout(K k, int sec) {
        ExpireWrap obj = map.get(k);
        if(obj != null) {
            synchronized (obj) {
                if(sec > 0) {
                    obj.expire = System.currentTimeMillis() + sec * 1000;
                } else {
                    //原本是不超时的对象, 设置超时
                    obj.expire = sec;
                    delayQueue.add(obj);
                }
            }
        }
    }

    public boolean isTimeout(K k) {
        ExpireWrap obj = map.get(k);
        if(obj == null) {
            return true;
        }
        return obj.isTimeout();
    }

    public Enumeration<K> keys() {
        return map.keys();
    }

    public int size() {
        return map.size();
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

    @Override
    public void run() {
        ExpireWrap v = null;
        while(isRun) {
            do {
                try {
                    v = delayQueue.poll(200, TimeUnit.MILLISECONDS);
                    if(v != null) {
                        v = map.get(v.k);
                    }
                    if(v != null) {
                        synchronized (v) {
                            if(v.isTimeout()) {
                                map.remove(v.k);
                                if(listener != null) {
                                    listener.timeout(this, v.k, v.obj);
                                }
                            } else if(v.expire > 0) {
                                //todo 优化此处的效率
                                if(!delayQueue.contains(v)) {
                                    delayQueue.add(v);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    v = null;
                }
            } while(v != null);
        }
        Utils.SLEEP(1);
    }

    @Override
    public void close() throws Exception {
        clear();
        isRun = false;
        runner.interrupt();
        runner.join();
        runner = null;
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
            return this.k.equals(other.k);
        }

        @Override
        public int compareTo(Delayed o) {
            ExpireWrap other = (ExpireWrap) o;
            return Long.compare(this.expire, other.expire);
        }
    }

    public interface TimeoutListener<K, V> {
        public void timeout(TimeoutConcurrentHashMap context, K k, V v);
    }
}
