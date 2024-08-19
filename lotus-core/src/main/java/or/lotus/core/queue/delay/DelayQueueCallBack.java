package or.lotus.core.queue.delay;

/**回调里面不要抛出异常*/
public abstract class DelayQueueCallBack {
    /**延迟队列初始化, 此处可以从数据库或本地文件读取未执行的任务到队列*/
    public void onInit(DelayQueueExecutor context) {

    }
    /**调用addTask时触发, 当抛出自动重试异常时, 不会触发此回调*/
    public void onAddTask(DelayQueueExecutor context, String type, long execTime, Object obj) throws Exception {

    }
    /**任务执行完毕时触发*/
    public void onSuccess(DelayQueueExecutor context, String type, long execTime, Object obj, String result) {

    }
    public void onRetryException(DelayQueueExecutor context, String type, long execTime, Object obj) {

    }
    public void onUnknownException(DelayQueueExecutor context, String type, long execTime, Object obj, Exception e) {

    }

}
