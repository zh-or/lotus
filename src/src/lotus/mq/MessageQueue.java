package lotus.mq;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import lotus.util.Util;

/**
 * 用来检测消息超时的类
 * @author OR
 */
public class MessageQueue implements Runnable{
    
    private ArrayList<IMQBase>                  msg_sent;
    private ReentrantLock                       lock_msg_sent;
    private BlockingQueue<Object>               msg_receipt;//回执队列
    private MessageTimeOut                      tcallback;
    private ExecutorService                     expool;
    
    public interface MessageTimeOut{
        public void timeisup(IMQBase msg);
    }

    /**
     * @param tcallback 消息超时时回调
     * @param capacity 回执队列大小
     */
    public MessageQueue(MessageTimeOut tcallback, int capacity){
        this.tcallback          = tcallback;
        this.lock_msg_sent      = new ReentrantLock();
        this.msg_sent           = new ArrayList<IMQBase>();
        this.msg_receipt        = new ArrayBlockingQueue<Object>(capacity);
        this.expool             = Executors.newSingleThreadExecutor();
        
        Thread thread = new Thread(this);
        thread.setName("thread-MessageQueue");
        thread.start();
    }

    public void close(){
        lock_msg_sent.lock();
        try {
            for(int i = (msg_sent.size() - 1); i >= 0; i --){
                expool.execute(new TimeOutRun(msg_sent.get(i), tcallback));
            }
            msg_sent.clear();
        } finally{
            lock_msg_sent.unlock();
        }
        msg_receipt.clear();
    }
    
    public void print(){
        System.out.println(String.format("消息大小: %d, 回执大小: %d", msg_sent.size(), msg_receipt.size()));
    }
    
    /**
     * 添加一条已发送的消息
     * @param msg
     */
    public void addMessage(IMQBase msg){
        synchronized (lock_msg_sent) {
            msg_sent.add(msg);
        }
    }
    
    /**
     * 添加回执
     * @param obj
     */
    public void addReceipt(Object obj){
        msg_receipt.add(obj);
    }
    
    public IMQBase getMessage(Object obj){
        IMQBase tmp = null;
        synchronized (lock_msg_sent) {
            for(int i = (msg_sent.size() - 1); i >= 0; i --){
                if(msg_sent.get(i).equals(obj)){
                    tmp = msg_sent.get(i);
                }
            }
        }
        return tmp;
    }
    
    public IMQBase remove(Object tag){
        IMQBase tmp = null;
        synchronized (lock_msg_sent) {
            for(int i = (msg_sent.size() - 1); i >= 0; i --){
                if(msg_sent.get(i).equals(tag)){
                    tmp = msg_sent.remove(i);
                }
            }
        }
        return tmp;
        
    }
    
    @Override
    public void run() {
        long st = 0l;
        long et = 0l;
        long sleeptime = 0;
        while(true){
            sleeptime =  et - st;
            if(sleeptime < 100){
                sleeptime = 100 - sleeptime;
                Util.SLEEP(sleeptime);
            }
            Object msgid = msg_receipt.poll();
            st = System.currentTimeMillis();
            if(!msg_sent.isEmpty()){
                lock_msg_sent.lock();
                try {
                    IMQBase item = null;
                    for(int i = (msg_sent.size() - 1); i >= 0; i --){
                        item = msg_sent.get(i);
                        if(item == null) break;
                        if(msgid != null && item.equals(msgid)){
                            msg_sent.remove(i);
                        }else{
                            long t = item.getTimeOut();
                            t -= sleeptime;
                            if(t <= 0){
                                expool.execute(new TimeOutRun(item, tcallback));
                                msg_sent.remove(i);
                            }else{
                                item.setTimeOut(t);
                            }
                        }
                    }
                    if(item == null) break;
                }finally{
                    lock_msg_sent.unlock();
                }
            }else{
                Util.SLEEP(100);//防止死循环过度占用cpu
            }
            et = System.currentTimeMillis();
        }
    }
    
    private class TimeOutRun implements Runnable{
        private IMQBase msg;
        private MessageTimeOut tcb;
        
        public TimeOutRun(IMQBase msg, MessageTimeOut tcb) {
            this.msg = msg;
            this.tcb = tcb;
        }

        @Override
        public void run() {
            tcb.timeisup(msg);
        }
    }
    
}
