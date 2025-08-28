package or.lotus.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public class GraceShutdown {
    static Logger log = LoggerFactory.getLogger(GraceShutdown.class);
    public static boolean outCloseLog = false;
    private ArrayList<AutoCloseable> objs = new ArrayList<>();

    public synchronized void add(AutoCloseable obj) {
        objs.add(obj);
    }

    public void shutdown() {
        for(AutoCloseable obj : objs) {
            try {
                if(outCloseLog) {
                    log.info("call close: {}", obj.getClass().getName());
                }
                obj.close();
            } catch (Exception e) {
                log.error("关闭出错: {}, {}", obj, e);
            }
        }
    }
}
