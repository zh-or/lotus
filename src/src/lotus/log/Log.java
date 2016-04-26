package lotus.log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log implements ILog{
    public abstract class LogFilter{
        /**
         * @param lvl
         * @param logstr
         * @return 返回true表示拦截将不输出
         */
        public boolean log(int lvl, String logstr){return false;}
    }
    
    private static String    PROJECT_NAME         =    "";
    private static final String lvl[]             =   {"[INFO]", "[WARN]", "[ERROR]", "[DEBUG]"};
    private static SimpleDateFormat format        =   new SimpleDateFormat("MM-dd hh:mm:ss");
    
    private static Log log                        =   null;
    private static Object lock_obj                =   new Object();
    
    private LogFilter   logfilter   =   null;
    
    
    private Log(){
        logfilter = new LogFilter() {
        };
    }
    
    public static Log getInstance(){
        if(log == null){
            synchronized (lock_obj) {
                if(log == null){
                    log = new Log();
                }
            }
        }
        return log;
    }
    
    public void log(String str){
        log(L_I, str);
    }
    
    public void log(int l, String str, Object ...args){
        log(l, String.format(str, args));
    }
    
    public void log(int l, String str){
        String msg_ = format.format(new Date(System.currentTimeMillis()));
        msg_ = String.format("%s %s %s \t%s", msg_, PROJECT_NAME, lvl[l], str);
        if(!logfilter.log(l, msg_)){
            System.out.println(msg_);
        }
    }
    
    public void setProjectName(String name){
        PROJECT_NAME = "[" + name + "] ";
    }
    
    public void setLogFilter(LogFilter logfilter){
        this.logfilter = logfilter;
    }

    @Override
    public void info(String str) {
        log(str);
    }

    @Override
    public void info(String str, Object... args) {
        log(ILog.L_I, str, args);
    }

    @Override
    public void warn(String str) {
        log(ILog.L_W, str);
    }

    @Override
    public void warn(String str, Object... args) {
        log(ILog.L_W, str, args);
    }

    @Override
    public void error(String str) {
        log(ILog.L_E, str);
    }

    @Override
    public void error(String str, Object... args) {
        log(ILog.L_E, str, args);
    }

    @Override
    public void debug(String str) {
        log(ILog.L_D, str);
    }

    @Override
    public void debug(String str, Object... args) {
        log(ILog.L_D, str, args);
    }
}
