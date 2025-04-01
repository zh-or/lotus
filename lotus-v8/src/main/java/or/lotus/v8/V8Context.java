package or.lotus.v8;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import or.lotus.core.common.Utils;
import or.lotus.core.intmap.SparseArray;
import or.lotus.v8.lib.*;
import or.lotus.v8.support.JavaLibBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author or
 * js 与java通讯使用String是最快的方式, 用 v8object 可能会有内存泄漏
 */
public abstract class V8Context implements Runnable {
    protected static final Logger log = LoggerFactory.getLogger(V8Context.class);
    protected static final int waitJsResultTime = 10 * 1000;//等待js执行获取结果时间

    protected String                 name                 = null;
    protected V8MessageLoopObj       mq                   = null;
    protected volatile boolean jsRun = false;
    protected volatile boolean jsQuit = false;
    protected Object lockWaitClose = new Object();
    protected V8Context              self                 = null;
    protected Thread                 thread               = null;
    protected V8                     v8                   = null;
    protected ArrayList<JavaLibBase> javaLibs             = null;
    protected static AtomicInteger   messageIdPlus        = new AtomicInteger(20);
    protected ArrayList<String>      libPaths             = new ArrayList<String>(5);

    protected boolean                debug                = false;

    protected SparseArray<V8Object> syncFreeObj           = null;

    private static final String V8_MAX_SPACE_SIZE = " --max-old-space-size=4096";

    protected String jniLibPath = null;

    public V8Context() {
        this(null, V8_MAX_SPACE_SIZE);
    }

    public V8Context(String jniLibPath, String v8Flags) {
        this.jniLibPath = jniLibPath;
        this.jsQuit = false;
        this.mq = new V8MessageLoopObj();
        this.syncFreeObj = new SparseArray<>();
        this.javaLibs = new ArrayList<JavaLibBase>();
        try {
            this.addJavaLib(new V8RequireLib());
            this.addJavaLib(new V8LogLib());
            this.addJavaLib(new V8HttpLib());
            this.addJavaLib(new V8TimerLib());
            this.addJavaLib(new V8SqliteLib());

            if(v8Flags != null) {
                if(v8Flags.indexOf("--max-old-space-size") == -1) {
                    v8Flags += V8_MAX_SPACE_SIZE;
                }
                V8.setFlags(v8Flags);
            } else {
                V8.setFlags(V8_MAX_SPACE_SIZE);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册消息ID, 所有lib都应该用这个方法来注册消息id, 以防止重复.
     * @return
     */
    public static int getMessageId() {
        messageIdPlus.compareAndSet(0x6fffffff, 1000);
        return messageIdPlus.getAndIncrement();
    }

    public void addJavaScriptLibPath(String path) {
        libPaths.add(path);
    }

    public ArrayList<String> getJavaScriptLibPaths() {
        return libPaths;
    }

    /**
     * 添加需要异步释放内存的对象,
     * 由于异步操作, 停止时可能还没有释放, 由 freeAllSyncObj 手动释放全部
     * @param obj
     */
    public void addSyncObj(V8Object obj) {
        if(obj == null) {
            return;
        }
        if(obj.isReleased()){
            return;
        }
        syncFreeObj.put(obj.hashCode(), obj);
    }

    public void removeSyncObj(V8Object obj) {
        syncFreeObj.remove(obj.hashCode());
    }

    public void freeAllSyncObj() {
        for(int i = 0, nsize = syncFreeObj.size(); i < nsize; i++) {
            V8Object obj = syncFreeObj.valueAt(i);
            if(!obj.isReleased()) {
                obj.close();
            }
        }
    }

    /**
     * 需要在init之前注册
     * @param lib
     * @throws Exception  如果未在 init 之前调用则会爆出异常
     */
    public void addJavaLib(JavaLibBase lib) throws Exception{
        if(jsRun){
            throw new Exception("注册Java库需要在 init() 之前调用");
        }

        this.javaLibs.add(lib);
    }

    /**
     * 此方法将在内部new Thread, 如果需要使用自定义的线程池 请使用 {@link init}
     * @param name
     */
    public void init(String name) {
        this.init(name, true);
    }

    /**
     *
     * @param name
     * @param useSelfThread 使用false初始化, 将不会直接允许js, 需要附加到外部线程, 需要注意的是线程不能切换, 当前上下文只能在一个线程内运行
     */
    public void init(String name, boolean useSelfThread) {
        this.self = this;
        this.name = name;
        this.jsRun = true;
        this.jsQuit = false;
        if(useSelfThread){
            this.thread = new Thread(this, name);
            this.thread.start();
        }

    }

    public String getName(){
        return this.name;
    }

    /**
     * 加载JavaScript文件
     * @param file
     */
    public void loadJavaScriptFile(File file) throws IOException {
        String js = new String(Files.readAllBytes(file.toPath()), "utf-8");
        execJavaScript(js, file.getName());
    }

    public void execJavaScript(String js, String name) {
        execJavaScript(js, name, false);
    }

    public boolean execJavaScript(String js, String name, boolean isWaitRes) {
        Message msg = new Message(Message.LOAD);
        msg.setAttr1(js);
        msg.setAttr2(name);
        pushMessage(msg);

        if(!isWaitRes) {
            return true;
        }

        msg.waitResult(30 * 1000);

        return msg.getResult() != null;
    }

    /**
     * 结束 会等待结束
     * @param  timeout the maximum time to wait in milliseconds.
     * @return
     */
    public boolean quit(long timeout) {
        if(!jsRun) return true;
        try {
            synchronized (lockWaitClose) {
                quit();
                lockWaitClose.wait(timeout);
            }
            return true;
        } catch (InterruptedException e) {

        }
        return false;
    }

    /**
     * 结束 不会等待结束
     */
    public void quit() {
        pushMessage(Message.createQuit());
    }

    public boolean isRuning(){
        return jsRun;
    }

    public boolean isQuiting(){
        return jsQuit;
    }

    public void DEBUG(boolean enable){
        this.debug = enable;
    }

    public boolean DEBUG(){
        return this.debug;
    }


    /**
     * 检查当前js是否运行
     * @return 返回false表示当前js的主线程已经停止运行
     */
    public boolean checkRuning(){
        return jsRun;
    }

    public V8 getRuntimer() {
        return v8;
    }

    public Object call(String functionName) throws Exception{
        return call(functionName, null);
    }

    /**
     * 调用
     * @param functionName
     * @param pars 数组
     * @return 如果返回的是v8对象 则必须调用该对象的 close 方法, 手动释放内存.
     * @throws Exception
     */
    public Object call(String functionName, Object[] pars) throws Exception {
        if(!jsRun || jsQuit) {
            //当js正在停止时如果不加判断会导致调用方等待超时 wait_js_run_time
            throw new Exception(String.format("call %s (%s) 失败, 当前js正在停止...", functionName, pars));
        }
        Message msg = new Message(Message.CALL);
        msg.setAttr1(functionName);
        msg.setAttr2(pars);
        pushMessage(msg);
        msg.waitResult(waitJsResultTime);
        return msg.getResult();
    }

    public void selfCall(String functionName, Object[] pars) {


    }

    @Override
    public void run() {

        try{
            //v8Base 为顶层对象, 不能用 global 这个字符串？
            //需要判断是否windows, windows时释放dll
            v8 = V8.createV8Runtime("v8Base", null);

            for(JavaLibBase lib : javaLibs) {
                lib.onInit(self);
            }

            v8.registerJavaMethod(self, "quit", "quit", new Class<?>[] {});
            //v8.registerJavaMethod(self, "require", "require", new Class<?>[] {String.class});

            onCreate();
        } catch(Throwable e) {
            jsRun = false;
            e("策略 [%s] 启动失败:%s", name, Utils.formatException(e));
        }

        while(jsRun){
            Message msg = mq.getOne(200);
            if(msg != null){
                try{
                    int type = msg.getType();
                    switch(type){
                        case Message.CALL:
                        {
                            Object ret = null;
                            try {
                                ret = v8.executeJSFunction((String) msg.getAttr1(), (Object[]) msg.getAttr2());
                            } catch(Throwable e) {
                                throw e;
                            } finally {
                                msg.setResult(ret);
                            }
                            break;
                        }
                        case Message.LOAD:
                            try {
                            	String script = (String) msg.getAttr1();
                            	String scriptName = (String) msg.getAttr2();
                            	//加上两个空格不然异常时名字连到一起了.
                            	v8.executeVoidScript(script, " FileName:" + scriptName, 0);
                            	if(scriptName != null && !"".equals(scriptName)) {
                                    onLoaded(scriptName);
                                }
                                msg.setResult(true);
                                break;
                            } catch(Throwable e) {
                                onError(e);
                            }
                            msg.setResult(false);
                            break;
                        case Message.QUIT:
                            jsQuit = true;

                            for(JavaLibBase lib : javaLibs) {
                                lib.onQuit();
                            }
                            break;
                        default:
                            boolean isDg = false;
                            for(JavaLibBase lib : javaLibs) {
                                if(lib.MessageLoop(msg)) {
                                    isDg = true;
                                    break;
                                }
                            }
                            if(!isDg) {
                                MessageLoop(msg);
                            }
                            break;
                    }
                } catch(Exception e) {
                    onError(e);
                }
            }
            Utils.SLEEP(1);
            if(jsQuit){
                jsRun = false;
                break;
            }
        }
        try{
            for(JavaLibBase lib : javaLibs) {
                lib.onDestroy();
            }
            onDestroy();
        }catch(Throwable e){
            onError(e);
        }
        mq.clear();

        try {

            if(v8 != null && !v8.isReleased()) v8.close();
        }catch(Throwable e) {
            onError(e);
        }


        jsRun = false;
        synchronized (lockWaitClose) {
            lockWaitClose.notifyAll();
        }
    }

    public void pushMessage(Message msg) {
        if(!isRuning()) {
            return;
        }
        mq.push(msg);
    }

    public void onError(Throwable e){
        e("JavaScript Error:%s", Utils.formatException(e));
    }

    public void onLoaded(String scriptName) {

    }

    /**
     * js 主线程回调, 可直接使用v8, 此处还未加载js文件 用于注册JAVA方法到js运行时
     */
    protected abstract void onCreate();

    /**
     * js 主线程回调, 可直接使用v8
     * 不管是否初始化成功 (js语法错误等初始化失败时 不会调用onCreate) 都会调用此方法
     */
    protected abstract void onDestroy();

    /**
     * js 主线程回调, 可直接使用v8
     */
    protected abstract void MessageLoop(Message msg);


    public abstract void runSyncTask(Runnable run);



    public void i(String str) {
        log.info(str);
    }

    public void i(String str, Object... args) {
        log.info(str, args);
    }

    public void w(String str) {
        log.warn(str);
    }

    public void w(String str, Object... args) {
        log.warn(str, args);
    }

    public void e(Object str) {
        log.error(str.toString());
    }

    public void e(Object str, Object... args) {
        log.error(str.toString(), args);
    }

    public void d(String str) {
        log.debug(str);
    }

    public void d(String str, Object... args) {
        log.debug(str, args);
    }

}
