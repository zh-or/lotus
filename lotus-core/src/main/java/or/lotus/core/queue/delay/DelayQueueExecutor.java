package or.lotus.core.queue.delay;


import or.lotus.core.common.Format;
import or.lotus.core.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.*;

/**
 * 定时任务执行类, 如果执行任务时执行器抛出 RetryDelayTaskException 异常则会自动重试,
 * 并于 当前时间 + RetryDelayTaskException.getNextSec() * 1000 后再次执行,
 * 再次执行时不会在数据库类新加数据记录,
 * 如果抛出其他异常则不会自动重试
 * */
public class DelayQueueExecutor implements Runnable, Closeable {
    static final Logger log = LoggerFactory.getLogger(DelayQueueExecutor.class);

    private DelayQueue<DelayObj> queue;
    private ConcurrentHashMap<String, DelayTaskExec> execMap = new ConcurrentHashMap<>();
    private ExecutorService executorPoll;

    private DelayQueueCallBack callback;

    private boolean isRun = false;

    public DelayQueueExecutor(DelayQueueCallBack callback) {
        /*try (DelayQueueService queueService = new DelayQueueService()) {
            ArrayList<LinkDelayQueue> arr = queueService.queryWaitExecTask();

            for (LinkDelayQueue obj : arr) {
                queue.add(new DelayObjWrap(obj));
            }
            log.info("已添加初始任务: {}", arr.size());

        }*/
        this.callback = callback;
        queue = new DelayQueue<>();
        if(this.callback != null) {
            this.callback.onInit(this);
        }
        isRun = true;
        executorPoll = Executors.newFixedThreadPool(50);
        Thread thread = new Thread(this);
        thread.setName("DelayQueueExecutor-thread");
        thread.start();
    }

    /**name将用来判断重复*/
    public void addTask(String taskType, long execTime, Object obj) throws Exception {
        if(isRun) {//没有初始化完毕时 不调用添加回调
            if(this.callback != null) {
                this.callback.onAddTask(this, taskType, execTime, obj);
            }
        }

        try {
            DelayObj task = new DelayObj(taskType, execTime, obj);
            if(!queue.add(task)) {
                log.error("添加延迟任务到数据成功, 但是添加到队列失败:{}", task);
            }
        } catch (Exception e) {
            log.error("添加任务出错: ", e);
        }
    }

    /**按名字检查任务是否存在*/
    public boolean containsTask(Object obj) {
        return queue.contains(obj);
    }

    /**注册指定任务类型的定时任务执行器*/
    public void registerTaskExec(String taskType, DelayTaskExec exec) throws Exception {
        if(execMap.containsKey(taskType)) {
            throw new Exception("重复注册执行器:" + taskType);
        }
        execMap.put(taskType, exec);
    }

    public int getTaskTotal() {
        return queue.size();
    }

    @Override
    public void close() {
        isRun = false;
        executorPoll.shutdown();
    }

    @Override
    public void run() {
        Utils.SLEEP(1000);
        log.info("延迟队列开始运行...");

        while(isRun) {
            try {
                DelayObj delayObj = queue.poll(1, TimeUnit.SECONDS);
                if(delayObj != null) {
                    DelayTaskExec exec = execMap.get(delayObj.getTaskType());
                    if(exec == null) {
                        log.error("延迟队列没有注册执行器, type: {}, obj: {}", delayObj.type, delayObj.obj);
                        continue;
                    }

                    executorPoll.execute(new TaskRunner(delayObj, exec));
                }
            } catch (Exception e) {

            }
            Utils.SLEEP(1);
        }
    }



    public class DelayObj implements Delayed {
        public Object obj;
        public String type;
        public long execTime;

        public DelayObj(String type, long execTime, Object obj) {
            this.type = type;
            this.execTime = execTime;
            this.obj = obj;
        }

        public String getTaskType() {
            return type;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            //延迟任务是否到时就是按照这个方法判断如果返回的是负数则说明到期
            // 否则还没到期
            return unit.convert(execTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            DelayObj other = (DelayObj) o;
            return (int)((execTime - System.currentTimeMillis()) - (other.execTime - System.currentTimeMillis()));
        }

        @Override
        public boolean equals(Object obj) {
            DelayObj other = (DelayObj) obj;
            return this.obj.equals(other.obj);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("type=").append(type)
                    .append(", execTime=").append(execTime)
                    .append(", execTime=").append(Format.formatTime(execTime))
                    .append(", obj=").append(obj);
            return sb.toString();
        }
    }


    private class TaskRunner implements Runnable {
        DelayObj delayObj;
        DelayTaskExec exec;
        public TaskRunner(DelayObj delayObj, DelayTaskExec exec) {
            this.delayObj = delayObj;
            this.exec = exec;
        }


        @Override
        public void run() {
            try {
                String res = exec.exec(DelayQueueExecutor.this, delayObj.type, delayObj.obj);
                if(callback != null) {
                    callback.onSuccess(DelayQueueExecutor.this, delayObj.type, delayObj.execTime, delayObj.obj, res);
                }
            } catch(DelayQueueRetryException re) {
                //自动重试任务, 重试时不添加新任务
                try {
                    delayObj.execTime = System.currentTimeMillis() + re.getNextSec() * 1000;
                    queue.add(delayObj);
                    if(callback != null) {
                        callback.onRetryException(DelayQueueExecutor.this, delayObj.type, delayObj.execTime, delayObj.obj);
                    }
                    //addTask(delayObjWrap.obj.getTaskType(), delayObjWrap.obj.getName(), System.currentTimeMillis() + 1000 * re.getNextSec(), delayObjWrap.obj.getParams());
                } catch (Exception e) {
                    log.error("自动重试任务添加失败,  type: {}, obj: {}, e: {}",
                            delayObj.type,
                            delayObj.obj,
                            e);
                }
            } catch(Exception e) {
                if(callback != null) {
                    callback.onUnknownException(DelayQueueExecutor.this, delayObj.type, delayObj.execTime, delayObj.obj, e);
                }
            }
        }
    }
}
