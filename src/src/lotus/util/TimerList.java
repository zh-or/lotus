package lotus.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimerList {
    private static final int THREAD_TOTAL   =   4;
    
    private boolean run                     = false;
    private ArrayList<TimerItem>  arr       = null;
    private Object lock                     = new Object();
    private ExecutorService runthread       = null;
    
    private Runnable timer_run              = new Runnable() {
        @Override
        public void run() {
            long st, et, d;
            int i = 0, size;
            
            while(run){
                st = System.currentTimeMillis();
                synchronized (lock) {
                    size = arr.size() - 1;
                    if(size > 0){
                        for(i = size; i >= 0; i--){
                            TimerItem item = arr.get(i);
                            d = item.passing(1000);
                            if(d <= 0){
                                runthread.execute(new WorkRun(item));
                                arr.remove(i);
                            }
                        }
                    }
                }
                et = System.currentTimeMillis();
                Util.SLEEP((int) (1000 - (et - st)));
            }
        }
    };
    
    private class WorkRun implements Runnable{
        private TimerItem item;
        
        public WorkRun(TimerItem item) {
            this.item = item;
        }
        
        @Override
        public void run() {
            boolean isr = item.getCallBack().onup(item.getInitialtime(), item.getTag());
            if(!isr){
                TimerList.this.add(item.getTag(), item.getInitialtime(), item.getCallBack());
            }
        }
    }
    
    public TimerList(){
        arr = new ArrayList<TimerList.TimerItem>();
        runthread = Executors.newFixedThreadPool(THREAD_TOTAL);
    }
    
    public void start(){
        run = true;
        new Thread(timer_run).start();
    }
    
    public void stop(){
        run = false;
        synchronized (lock) {
            arr.clear();
        }
    }
    
    public void add(Object tag, long delay, TimeIsUp callback){
        synchronized (lock) {
            arr.add(new TimerItem(tag, delay, callback));
        }
    }
    
    public Object remove(Object tag){
        if(tag == null) return null;
        Object obj = null;
        synchronized (lock) {
            int i = 0, size;
            size = arr.size() - 1;
            for(i = size; i >= 0; i--){
                TimerItem item = arr.get(i);
                obj = item.getTag();
                if(tag.equals(obj)){
                    arr.remove(i);
                }
            }
        }
        return obj;
    }
    
    private class TimerItem{
        private long time_delay = 0;
        private long time_delay_initial  = 0;
        private Object tag = 0;
        private TimeIsUp cb;
        
        public TimerItem(Object tag, long delay, TimeIsUp callback){
            this.time_delay = delay;
            this.tag = tag;
            this.cb = callback;
        }
 
        public long passing(int t){
            time_delay -= t;
            return time_delay;
        }
        
        public long getInitialtime(){
            return time_delay_initial;
        }
        
        public Object getTag(){
            return tag;
        }
        
        public TimeIsUp getCallBack(){
            return cb;
        }
    }
    
    public interface TimeIsUp{
        /**
         * @param initialtime
         * @param tag
         * @return 返回true则表示处理完毕, 返回false则表示下次继续
         */
        public boolean onup(long initialtime, Object tag);
    }
}
