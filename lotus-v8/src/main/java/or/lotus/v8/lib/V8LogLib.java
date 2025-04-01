package or.lotus.v8.lib;

import or.lotus.v8.Message;
import or.lotus.v8.V8Context;
import or.lotus.v8.support.JavaLibBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

public class V8LogLib extends JavaLibBase {
    protected static final Logger log = LoggerFactory.getLogger(V8Context.class);
    private V8Context base = null;
    private V8 runtime = null;

    @Override
    public void onInit(V8Context v8b) {
        base = v8b;

        runtime = v8b.getRuntimer();
        V8Object log = new V8Object(runtime);
        log.registerJavaMethod(base, "i", "i", new Class<?>[] { String.class, Object[].class });
        log.registerJavaMethod(base, "e", "e", new Class<?>[] { Object.class, Object[].class });
        log.registerJavaMethod(base, "d", "d", new Class<?>[] { String.class, Object[].class });
        log.registerJavaMethod(base, "w", "w", new Class<?>[] { String.class, Object[].class });
        runtime.add("log", log);
        log.close();
    }



    @Override
    public void onQuit() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean MessageLoop(Message msg) {
        return false;
    }

}
