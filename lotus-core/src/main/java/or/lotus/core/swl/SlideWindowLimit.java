package or.lotus.core.swl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class SlideWindowLimit extends TimerTask {

    protected static Logger log = LoggerFactory.getLogger(SlideWindowLimit.class);
    protected SlideWindowData slideWindowData;
    protected ConcurrentHashMap<String, SlideWindowObj> mem;
    protected Timer timer;
    protected int timeoutSec;
    int limit;
    int limitSec;
    int limitCount;
    static final String KEY_PREFIX = "slide_window_limit:";

    /**
     * @param limit 窗口大小
     * @param limitSec 窗口时间间隔长度
     * @param limitCount 窗口时间长度内允许的最大请求次数
     */
    public SlideWindowLimit(int limit, int limitSec, int limitCount) {
        this(null, limit, limitSec, limitCount);
    }

    public SlideWindowLimit(SlideWindowData slideWindowData, int limit, int limitSec, int limitCount) {
        this.slideWindowData = slideWindowData;
        this.limit = limit;
        this.limitSec = limitSec;
        this.limitCount = limitCount;
        this.timeoutSec = limitSec * limitCount + 1;

        if(slideWindowData == null) {
            mem = new ConcurrentHashMap<>();
            timer = new Timer();
            timer.schedule(this, 5000, 5000);
        }
    }

    /**返回true表示未被限制, 返回false表示已限制*/
    public boolean check(String key) {
        return check(key, limitCount);
    }

    /**返回true表示未被限制, 返回false表示已限制*/
    public boolean check(String key, int  count) {
        SlideWindowObj val = getSlideWindowObj(key);
        //log.info("wnd: {}", val.toString());
        boolean flag = val.check(count);
        try {
            set(KEY_PREFIX + key, val);
        } catch (JsonProcessingException e) {
            log.error("滑动窗口保存数据到redis失败:", e);
        }
        return flag;
    }


    public SlideWindowObj getSlideWindowObj(String key) {
        SlideWindowObj val = get(key);
        if(val == null || val.windows == null || val.limitSec == 0) {
            val = new SlideWindowObj(limit, limitSec, limitCount);
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
            mem.put(KEY_PREFIX + key, obj);
        }
    }

    public void close() {
        if(timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void run() {
        if(mem != null) {
            for(String key : mem.keySet()) {
                SlideWindowObj val = mem.get(key);
                //移除已超时的滑动窗口对象
                if(System.currentTimeMillis() - val.lastTime > timeoutSec * 1000) {
                    mem.remove(key);
                }
            }
        }
    }
}
