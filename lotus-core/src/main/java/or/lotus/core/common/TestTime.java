package or.lotus.core.common;

import java.util.LinkedHashMap;

public class TestTime {
    static LinkedHashMap<String, TimeLog> map = new LinkedHashMap<>();
    String last = null;

    public void start(String name) {
        map.put(name, new TimeLog());
        last = name;
    }

    public void end() {
        end("");
    }

    public void end(String append) {
        TimeLog log = map.get(last);
        if(log != null) {
            log.end(append);
        }
    }

    public void print() {
        for(String name : map.keySet()) {
            TimeLog log = map.get(name);
            System.out.println("[" + name + "]\t\t\t = " + (log.end - log.start) + "ms " + (Utils.CheckNull(log.append) ? "" : ">>>" + log.append));
        }
    }

    private class TimeLog {
        long start;
        long end;
        String append;

        TimeLog() {
            start = System.currentTimeMillis();
        }

        void end(String append) {
            end = System.currentTimeMillis();
            this.append = append;
        }
    }
}
