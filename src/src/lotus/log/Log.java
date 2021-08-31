package lotus.log;


public class Log {

    public static final int L_E        = 0;
    public static final int L_W        = 1;
    public static final int L_I        = 2;
    public static final int L_D        = 3;
    public static final int L_T        = 4;

    static final String lvl[]          =   {"[ERROR]", "[WARN] ", "[INFO] ", "[DEBUG]", "[TRACE]"};
    
    public interface LogFilter{
        /**
         * @param lvl
         * @param logstr
         * @return 返回false表示拦截, 将不输出
         */
        public boolean log(int lvl, String logstr);
    }
    
    private boolean          debug_enable         =   false;
    private String			 clazzName			  =	  null;

    private static Log      log                   =   null;
    private static Object   lock_obj              =   new Object();
    private LogFormat       logFormat             =   null;
    
    private Log(Class<?> clazz) {
        if(clazz != null) {
        	this.clazzName = clazz.getName();
        }
        
        if(logFormat == null) {
            logFormat = LogFormat.getLogFormat();
        }
        
        debug_enable = logFormat.getDebugEnable();
    }

    public static Log getLogger(){
        
        return getLogger(null);
    }
    
    public static Log getLogger(Class<?> clazz) {
        
        if(log == null) {
            synchronized (lock_obj) {
                if(log == null){
                    log = new Log(null);
                }
            }
        }
        if(clazz != null) {
            return new Log(clazz);
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
        logFormat.print(l, clazzName, str);
    }
    
    /**
     * 设置时区 
     * @param id GMT+8
     */
    public void setTimeZoneID(String id) {
        logFormat.setTimeZoneID(id);
    }
    
    public void setProjectName(String name){
        logFormat.setProjectName(name);
    }
    
    public void setDebugEnable(boolean isEnable){
        debug_enable = isEnable;
        logFormat.setDebugEnable(isEnable);
    }

    public void setLogFilter(LogFilter logfilter){
        logFormat.setLogFilter(logfilter);
    }
    
    /**
     * 设置日志文件输出目录
     * @param dir
     */
    public void setLogFileDir(String dir) {
        logFormat.setLogFileDir(dir);
    }

    public void info(String str) {
        log(str);
    }

    public void info(String str, Object... args) {
        log(L_I, str, args);
    }

    
    public void warn(String str) {
        log(L_W, str);
    }

    public void warn(String str, Object... args) {
        log(L_W, str, args);
    }

    public void error(String str) {
        log(L_E, str);
    }

    public void error(String str, Object... args) {
        log(L_E, str, args);
    }

    public void debug(String str) {
        if(debug_enable){
            log(L_D, str); 
        }
    }

    public void debug(String str, Object... args) {
        if(debug_enable){
            log(L_D, str, args);
        }
    }

    public void trace(String str) {
        log(L_T, str);
    }

    public void trace(String str, Object... args) {
        log(L_T, str, args);
    }
}
