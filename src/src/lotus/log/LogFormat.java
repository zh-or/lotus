package lotus.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lotus.log.Log.LogFilter;

public class LogFormat {

    private static LogFormat format               =   null;
    private static Object    _lock                =   new Object();
    private TimeZone         timeZone             =   null;
    private LogWriter        writer               =   null;
    private LogFilter        logfilter            =   null;
    private boolean          debug_enable         =   true;

    private String           PROJECT_NAME         =   "";

    public static LogFormat getLogFormat() {
        if(format == null) {
            synchronized (_lock) {
                if(format == null) {
                    format = new LogFormat();
                }
            }
        }
        return format;
    }

    /**
     * 这只日志文件保存目录, 按天保存
     * @param dir
     */
    public synchronized void setLogFileDir(String dir) {
        if(writer == null) {
            writer = new LogWriter();
        }
        writer.setDir(dir);
    }


    /**
     * 设置时区
     * @param id GMT+8
     */
    public void setTimeZoneID(String id) {
        timeZone = TimeZone.getTimeZone(id);
    }

    public void setProjectName(String name) {
        PROJECT_NAME = "[" + name + "]";
    }

    public void setLogFilter(LogFilter logfilter) {
        this.logfilter = logfilter;
    }

    public boolean getDebugEnable() {
        return debug_enable;
    }

    public void setDebugEnable(boolean isEnable) {
        debug_enable = isEnable;
    }

    private static final String NULL_STR = "";

    public void print(int l, String clazzName, String str) {
        String cname = NULL_STR;
        if(clazzName != null){
            cname = "[" + clazzName + "]";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss");
        if(timeZone != null) {
            sdf.setTimeZone(timeZone);
        }
        String msg_ = sdf.format(new Date());
        msg_ = String.format("%s %s%s%s %s", msg_, PROJECT_NAME, cname, Log.lvl[l], str);
        if(logfilter == null || logfilter.log(l, msg_)) {
            if(writer != null) {//写到文件
                writer.write(msg_);
            }
            System.out.println(msg_);
            System.out.flush();
        }
    }

}
