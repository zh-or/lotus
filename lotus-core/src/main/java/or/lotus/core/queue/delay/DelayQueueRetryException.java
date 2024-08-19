package or.lotus.core.queue.delay;

/**抛出此异常时会自动再添加到延迟队列里面*/
public class DelayQueueRetryException extends Exception{
    int sec;

    public DelayQueueRetryException(String message, int sec) {
        super(message);
        this.sec = sec;
    }

    public int getNextSec() {
        return sec;
    }
}
