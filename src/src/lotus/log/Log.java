package lotus.log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log implements ILog{
    public interface LogFilter{
        /**
         * @param lvl
         * @param logstr
         * @return 返回false表示拦截, 将不输出
         */
        public boolean log(int lvl, String logstr);
    }
    
    private static String           PROJECT_NAME         =    "";
    private static SimpleDateFormat format               =   new SimpleDateFormat("MM-dd HH:mm:ss");
    private static boolean          enable_class         =   true;
    private static boolean          trace_enabled        =   false;
    private static boolean          debug_enable         =   false;
    private static Log              log                  =   null;
    private static Object           lock_obj             =   new Object();
    private Class<?> 			    clazz				 =   null;
    private String					clazzName			 =	 null;
    
    private LogFilter   logfilter   =   null;
    
    
    private Log(Class<?> clazz){
        this.clazz = clazz;
        if(this.clazz != null) {
        	this.clazzName = this.clazz.getName();
        }
    }

    public static Log getInstance(){
        
        return getInstance(null);
    }
    
    public static Log getInstance(Class<?> clazz){
        if(log == null){
            synchronized (lock_obj) {
                if(log == null){
                    log = new Log(clazz);
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
    
    private static final String NULL_STR = "";
    public void log(int l, String str){
        String cname = NULL_STR;
        if(enable_class){
            
            cname = "[" + clazzName + "]";
        }
        
        String msg_ = format.format(new Date(System.currentTimeMillis()));
        msg_ = String.format("%s %s%s%s %s", msg_, PROJECT_NAME, cname, lvl[l], str);
        if(logfilter == null || logfilter.log(l, msg_)){
            System.out.println(msg_);
            System.out.flush();
        }
    }
    
    public void setProjectName(String name){
        PROJECT_NAME = "[" + name + "]";
    }
    
    public boolean isTraceEnabled(){
        return trace_enabled;
    }

    public void setTraceEnable(boolean isEnable){
        trace_enabled = isEnable;
    }

    public void setDebugEnable(boolean isEnable){
        debug_enable = isEnable;
    }
    
    public void setEnableClassNameOut(boolean enable){
        enable_class = enable;
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
        if(debug_enable){
            log(ILog.L_D, str); 
        }
    }

    @Override
    public void debug(String str, Object... args) {
        if(debug_enable){
            log(ILog.L_D, str, args);
        }
    }

    @Override
    public void trace(String str) {
        if(trace_enabled){
            log(ILog.L_T, str);
        }
    }

    @Override
    public void trace(String str, Object... args) {
        if(trace_enabled){
            log(ILog.L_T, str, args);
        }
    }
}
