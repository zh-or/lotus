package or.lotus.v8.lib;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import or.lotus.v8.src.V8;
import or.lotus.v8.src.V8Function;
import or.lotus.v8.Message;
import or.lotus.v8.V8Context;
import or.lotus.v8.support.JavaLibBase;

public class V8TimerLib extends JavaLibBase {
    private V8Context base = null;
    private V8 runtime = null;
    protected AtomicInteger interval_count = null;
    protected ConcurrentHashMap<String, V8Function> intervals = null;//定时器
    protected Timer timer = null;

    protected final int TIME_OUT = V8Context.getMessageId();
    protected final int TIME_INTERVAL = V8Context.getMessageId();
    protected final int TIME_INTERVAL_REMOVE = V8Context.getMessageId();

    @Override
    public void onInit(V8Context v8b) {
        interval_count = new AtomicInteger();
        timer = new Timer("v8-timer");
        intervals = new ConcurrentHashMap<String, V8Function>();
        base = v8b;


        runtime = v8b.getRuntimer();

        runtime.registerJavaMethod(this, "setTimeout", "setTimeout", new Class<?>[] {V8Function.class, int.class});
        runtime.registerJavaMethod(this, "setInterval", "setInterval", new Class<?>[] {V8Function.class, int.class});
        runtime.registerJavaMethod(this, "clearInterval", "clearInterval", new Class<?>[] {int.class});

    }


    @Override
    public void onQuit() {
        timer.cancel();
    }

    @Override
    public void onDestroy() {
        if(intervals != null && intervals.size() > 0){
            Iterator<Entry<String, V8Function>> it = intervals.entrySet().iterator();
            while(it.hasNext()){
                it.next().getValue().close();
                it.remove();
            }
        }
    }


    @Override
    public boolean MessageLoop(Message msg) {
        int type = msg.getType();
        if(type == TIME_OUT) {
            intervals.remove(msg.getAttr2());
            V8Function callback = (V8Function) msg.getAttr1();
            try {
                callback.call(runtime, null);
            } catch (Exception e) {
                //e("JavaScript运行出错, setTimeOut 回调函数内部错误:%s", V8T.formatV8ScriptExecution(e));
                base.onError(e);
            }finally{
                callback.close();
            }
            return true;
        }
        if(type == TIME_INTERVAL){
            V8Function callback = (V8Function) msg.getAttr1();
            try {
                callback.call(runtime, null);
            } catch (Exception e) {
                //e("JavaScript运行出错, setInterval 回调函数内部错误:%s", V8T.formatV8ScriptExecution(e));
                base.onError(e);
            }
            return true;
        }

        if(type == TIME_INTERVAL_REMOVE) {
            V8Function tf = intervals.remove(msg.getAttr1());
            if(tf != null) {
                tf.close();
            }
            return true;
        }

        return false;
    }

    public void setTimeout(V8Function callback, final int t) {
        if(base.isQuiting()) {
            return;
        }
        final V8Function newCallback = callback.twin();
        int id = interval_count.addAndGet(1);
        final String key = "______________TIMEOUT_ID_" + id;
        intervals.put(key, newCallback);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Message msg = new Message(TIME_OUT);
                msg.setAttr1(newCallback);
                msg.setAttr2(key);
                base.pushMessage(msg);
            }
        }, t);
    }

    public int setInterval(V8Function callback, final int t) {
        if(base.isQuiting()) {
            return -1;
        }
        V8Function newCallback = callback.twin();
        int id = interval_count.addAndGet(1);
        final String key = "______________INTERVAL_ID_" + id;
        intervals.put(key, newCallback);
        setInterval(key, t);
        return id;
    }

    public void clearInterval(int id){
        Message msg = new Message(TIME_INTERVAL_REMOVE);
        msg.setAttr1("______________INTERVAL_ID_" + id);
        base.pushMessage(msg);
    }

    public String setInterval(final String key, final int t){
        if(base.isQuiting()) {
            return null;
        }
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                V8Function tmp_newCallback = intervals.get(key);
                if(tmp_newCallback != null){
                    Message msg = new Message(TIME_INTERVAL);
                    msg.setAttr1(tmp_newCallback);
                    msg.setResult(t);
                    base.pushMessage(msg);
                    setInterval(key, t);
                }

            }
        }, t);
        return key;
    }



}
