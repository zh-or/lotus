package or.lotus.v8;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Message {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private int type;
    private Object msg;
    private Object result;
    private Object attr1;
    private Object attr2;
    private Object attr3;
    private Object attr4;
    private boolean isSetResult;



    public Message(int type) {
        this(type, null);
    }

    public Message(int type, Object msg) {
        this.type = type;
        this.msg = msg;
        this.result = null;
        this.isSetResult = false;
    }

    public void waitResult() {
        waitResult(0);
    }

    public void waitResult(long timeout) {
        lock.lock();
        try {
            try {
                condition.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } finally {
            lock.unlock();
        }
    }

    public Object getMsg() {
        return this.msg;
    }

    public int getType() {
        return this.type;
    }

    public void setResult(Object obj) {
        lock.lock();
        try {
            this.result = obj;
            this.isSetResult = true;
            condition.signalAll(); // 唤醒所有等待线程
        } finally {
            lock.unlock();
        }
    }

    public Object getResult(){
        return this.result;
    }

    public Object getLock(){
        return this;
    }

    public Object getAttr1() {
        return attr1;
    }

    public void setAttr1(Object attr1) {
        this.attr1 = attr1;
    }

    public Object getAttr2() {
        return attr2;
    }

    public void setAttr2(Object attr2) {
        this.attr2 = attr2;
    }

    public Object getAttr3() {
        return attr3;
    }

    public void setAttr3(Object attr3) {
        this.attr3 = attr3;
    }

    public void setAttr4(Object attr4) {
        this.attr4 = attr4;
    }

    public Object getAttr4() {
        return attr4;
    }

    public boolean isSetResult() {
        return isSetResult;
    }

    public static final int  QUIT            = 0;
    public static final int  CALL            = 1;
    public static final int  LOAD            = 2;

    public static Message createQuit() {
        return new Message(QUIT);
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", msg=" + msg + ", result=" + result + ", attr1=" + attr1 + ", attr2=" + attr2 +  "]";
    }



}
