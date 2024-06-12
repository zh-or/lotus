package or.lotus.queue.delay;

public abstract class DelayTaskExec<T> {
    public abstract String exec(DelayQueueExecutor exec, String type, T obj) throws Exception;
}
