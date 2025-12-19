package or.lotus.core.nio.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Promise {
    private PromiseRunner thenCall;
    private PromiseRunner exceptionCall;
    private PromiseRunner completeCall;
    private int gState = 0;//0:未完成 1:成功 2:失败 3:已调用
    private Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private Object params = null;
    private Throwable exception = null;

    public Object await() throws Throwable {
        return await(0);
    }

    public Object await(long timeout) throws Throwable {
        lock.lock();
        try {
            condition.await(timeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
        if(exception != null) {
            throw exception;
        }
        return params;
    }

    public Promise then(PromiseRunner run) {
        thenCall = run;
        checkCall(gState);
        return this;
    }

    public Promise exception(PromiseRunner run) {
        exceptionCall = run;
        checkCall(gState);
        return this;
    }

    public Promise complete(PromiseRunner run) {
        completeCall = run;
        checkCall(gState);
        return this;
    }

    public void callThen(Object params) {
        this.params = params;
        condition.notifyAll();
        checkCall(1);
    }

    public void callException(Throwable e) {
        exception = e;
        condition.notifyAll();
        checkCall(2);
    }


    protected void checkCall(int state) {
        lock.lock();
        try {
            gState = state;
            if(state == 1 && thenCall != null) {
                thenCall.run(params);
                if(completeCall != null) {
                    completeCall.run(null);
                }
                gState = 3;
            }
            if(state == 2 && exceptionCall != null) {
                exceptionCall.run(exception);
                if(completeCall != null) {
                    completeCall.run(null);
                }
                gState = 3;
            }
        } catch (Throwable e) {
            throw new RuntimeException("promise中需要手动处理全部异常:", e);
        } finally {
            lock.unlock();
        }
    }

    public interface PromiseRunner<T> {
        void run(T params);
    }
}
