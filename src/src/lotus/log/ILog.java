package lotus.log;


public interface ILog {
    
    public static final int L_I        = 0;
    public static final int L_W        = 1;
    public static final int L_E        = 2;
    public static final int L_D        = 3;
    
    public void log(String str);
    
    public void log(int l, String str, Object ...args);
    
    public void log(int l, String str);
    
    public void info(String str);
    
    public void info(String str, Object ...args);
    
    public void warn(String str);
    
    public void warn(String str, Object ...args);
    
    public void error(String str);
    
    public void error(String str, Object ...args);
    
    public void debug(String str);
    
    public void debug(String str, Object ...args);
}
