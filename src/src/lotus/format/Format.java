package lotus.format;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Format {
    
    public static String formatException(Throwable cause){
        StackTraceElement ste = cause.getStackTrace()[0];
        String exstr = String.format("filename:%s, info:%s, classname:%s, methodname:%s, line:%d", ste.getFileName(), cause.getMessage(), ste.getClassName(), ste.getMethodName(), ste.getLineNumber());
        return exstr;
    }
    
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static String formatTime(long time){
        return sdf.format(new Date(time));
    }
    
    public static String formatTime(Date date){
        return sdf.format(date);
    }
    
}
