package or.lotus.v8;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class V8MessageLoopObj {
    private LinkedBlockingQueue<Message> msg_queue = null;

    public V8MessageLoopObj() {
        msg_queue = new LinkedBlockingQueue<Message>(1000);
    }


    public void push(Message msg){
        msg_queue.add(msg);
    }

    /**
     *
     * @param msg
     * @param timeout the maximum time to wait in milliseconds.
     * @return
     */
    public Object pushAndWaitResult(Message msg, long timeout){
        Object lock = msg.getLock();
        push(msg);
        synchronized (lock) {
            if(!msg.isSetResult()){
                try {
                    lock.wait(timeout);
                } catch (InterruptedException e) { }
            }
        }
        return msg.getResult();
    }



    public Message getOne(int ms){
        Message msg = null;
        try {
            msg = msg_queue.poll(ms, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
        return msg;
    }

    public int size() {
        return msg_queue.size();
    }

    public void clear(){
        msg_queue.clear();
    }
}
