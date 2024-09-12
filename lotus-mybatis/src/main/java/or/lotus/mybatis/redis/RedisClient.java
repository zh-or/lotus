package or.lotus.mybatis.redis;

import or.lotus.core.common.BeanUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.Set;

public class RedisClient {
    private String password;
    private JedisPool pool;

    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);
    public RedisClient(String host, int port, String password) {
        this.password = password;
        /*redis = new Jedis(host, port, 5000);
        if(!Utils.CheckNull(password)) {
            redis.auth(password);
        }*/
        GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
        config.setMaxIdle(250);
        config.setMinIdle(0);
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(5));//空闲检测
        config.setTestWhileIdle(true);//输出连接时检测超时
        pool = new JedisPool(config, host, port, null, password);

    }

    public long removeKey(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.del(key);
        }
    }

    public long removeKeysByPattern(String pattern) {

        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys(pattern);
            if(keys != null && !keys.isEmpty()) {
                return jedis.del(keys.toArray(new String[keys.size()]));
            }
        }
        return 0;
    }

    public void close() {
        pool.close();
    }

    public Set<String> keys(String pattern) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.keys(pattern);
        }
    }

    public void putObj(String key, Object obj) throws JsonProcessingException {
        putObj(key, obj, -1);
    }

    public void putObj(String key, Object obj, int timeoutSec) throws JsonProcessingException {
        set(key, BeanUtils.ObjToJson(obj), timeoutSec);
    }

    public <T> T getObj(Class<T> clazz, String key) {
        try {
            String json = get(key);
            if(json != null && !json.isEmpty()) {
                return BeanUtils.JsonToObj(clazz, json);
            }
        } catch (JsonProcessingException e) {
            log.error("从 redis 获取对象出错: ", e);
        }
        return null;
    }

    public <T> T getObj(TypeReference<T> clazz, String key) {
        try {
            String json = get(key);
            if(json != null && !json.isEmpty()) {
                return BeanUtils.JsonToObj(clazz, json);
            }
        } catch (JsonProcessingException e) {
            log.error("从 redis 获取对象出错: ", e);
        }
        return null;
    }

    public long getLong(String key) {
        String v = get(key);
        if(v != null && !v.isEmpty()) {
            return Long.valueOf(v);
        }
        return 0;
    }

    public void setLong(String key, long val) {
        set(key, String.valueOf(val));
    }

    public int getInt(String key) {
        String v = get(key);
        if(v != null && !v.isEmpty()) {
            return Integer.valueOf(v);
        }
        return 0;
    }

    public void setInt(String key, int val) {
        set(key, String.valueOf(val));
    }

    public void setInt(String key, int val, int timeoutSec) {
        set(key, String.valueOf(val), timeoutSec);
    }

    public String get(String key) {
        try(Jedis redis = pool.getResource()) {
            return redis.get(key);
        }
    }

    public boolean exists(String key) {
        try(Jedis redis = pool.getResource()) {
            return redis.exists(key);
        }
    }

    public void set(String key, String val) {
        set(key, val, -1);
    }

    public void set(String key, String val, int timeoutSec) {

        try(Jedis redis = pool.getResource()) {
            redis.set(key, val);
            setTimeout(key, timeoutSec);
        }
    }

    /**
     * 设置自动过期时间
     * @param key
     * @param timeoutSec 传入-1时表示不过期, 传入单位为秒 seconds
     */
    public void setTimeout(String key, int timeoutSec) {

        try(Jedis redis = pool.getResource()) {
            if(timeoutSec > -1) {
                redis.expire(key, timeoutSec);
            } else {
                redis.persist(key);//永不过期
            }
        }
    }

    /**
     * 需要注意关闭
     * @return
     */
    public Jedis getRedis() {
        return pool.getResource();
    }
}
