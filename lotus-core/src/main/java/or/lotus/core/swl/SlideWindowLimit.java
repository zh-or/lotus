package or.lotus.core.swl;

import com.fasterxml.jackson.core.JsonProcessingException;
import or.lotus.core.common.TimeoutConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class SlideWindowLimit implements AutoCloseable {

    protected static Logger log = LoggerFactory.getLogger(SlideWindowLimit.class);
    protected SlideWindowData slideWindowData;
    protected TimeoutConcurrentHashMap<String, SlideWindowObj> mem;
    protected int timeoutSec;
    int limit;
    int limitSec;
    static final String KEY_PREFIX = "slide_window_limit:";

    /**
     * @param limit 窗口大小
     * @param limitSec 窗口时间间隔长度
     */
    public SlideWindowLimit(int limit, int limitSec) {
        this(null, limit, limitSec);
    }

    public SlideWindowLimit(SlideWindowData slideWindowData, int limit, int limitSec) {
        this.slideWindowData = slideWindowData;
        this.limit = limit;
        this.limitSec = limitSec;
        this.timeoutSec = limitSec * limit + 1;

        if(slideWindowData == null) {
            mem = new TimeoutConcurrentHashMap<>();
        }
    }

    /**返回true表示未达限制, 返回false表示已达限制
     * @Param key 键
     * @Param count 窗口内计数, 如果窗口内总数大于 count 则返回true, 否则返回false
     * */
    public boolean check(String key, int  count) {
        SlideWindowObj val = getSlideWindowObj(key);
        //log.info("wnd: {}", val.toString());
        boolean flag = val.check(count);
        try {
            set(key, val);
        } catch (JsonProcessingException e) {
            log.error("滑动窗口保存数据失败:", e);
        }
        return flag;
    }

    public void remove(String key) {
        if(this.slideWindowData != null) {
            this.slideWindowData.remove(KEY_PREFIX + key);
        } else {
            mem.remove(KEY_PREFIX + key);
        }
    }

    public SlideWindowObj getSlideWindowObj(String key) {
        SlideWindowObj val = get(key);
        if(val == null || val.windows == null || val.limitSec == 0) {
            val = new SlideWindowObj(limit, limitSec);
        }
        return val;
    }

    private SlideWindowObj get(String key) {
        if(this.slideWindowData != null) {
            return this.slideWindowData.getObj(SlideWindowObj.class, KEY_PREFIX + key);
        }
        return mem.get(KEY_PREFIX + key);
    }

    private void set(String key, SlideWindowObj obj) throws JsonProcessingException {
        if(this.slideWindowData != null) {
            this.slideWindowData.putObj(KEY_PREFIX + key, obj, timeoutSec);
        } else {
            mem.put(KEY_PREFIX + key, obj, timeoutSec);
        }
    }

    @Override
    public void close() throws Exception {
        if(mem != null) {
            mem.clear();
            mem.close();
        }
    }
}
