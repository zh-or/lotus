package or.lotus.v8.test;


import or.lotus.v8.Message;
import or.lotus.v8.V8Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Test extends V8Context {
    static Test v8;
    static ExecutorService exec;
    protected static final Logger log = LoggerFactory.getLogger(V8Context.class);

    public static void main(String[] args) throws Exception {
        exec = Executors.newFixedThreadPool(10, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "sqlite exec pool");
                return thread;
            }
        });


        v8 = new Test();
        v8.init("test");
        System.out.println("v8 test running..");
    }

    public Test() {

        super(null, null);
    }

    @Override
    protected void onCreate() {

        try {
            v8.loadJavaScriptFile(new File("./script/base.js"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        exec.shutdown();
    }

    @Override
    protected void MessageLoop(Message msg) {

    }

    @Override
    public void runSyncTask(Runnable run) {
        exec.execute(run);
    }

    @Override
    public void onError(Throwable e) {
        //System.out.println(Format.formatException(e));
        e.printStackTrace();
    }

}
