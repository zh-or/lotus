package or.lotus.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

public class GraceShutdown {
    static Logger log = LoggerFactory.getLogger(GraceShutdown.class);
    private ArrayList<Closeable> objs = new ArrayList<>();

    public synchronized void add(Closeable obj) {
        objs.add(obj);
    }

    public void shutdown() {
        for(Closeable obj : objs) {
            try {
                log.info("关闭 {}", obj.getClass().getName());
                obj.close();
            } catch (IOException e) {
                log.error("关闭出错: {}, {}", obj, e);
            }
        }
    }
}
