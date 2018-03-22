package lotus.format;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Format {
    
    public static String formatException(Throwable cause){
        StackTraceElement[] stes = cause.getStackTrace();
        String message = cause.getMessage();
        StringBuffer sb = new StringBuffer();
        sb.append("\n  ");
        sb.append(cause.getClass().getName());
        if(message != null) {
            sb.append(message);
        }
        sb.append("\n");
        for(StackTraceElement ste : stes) {
            if(ste.getFileName() != null) {
                sb.append("    ");
                sb.append(ste.toString());
                sb.append("\n");
            }
            
        }

        Throwable ourCause = cause.getCause();
        
        if(ourCause != null) {
            sb.append("  Caused by ");
            sb.append(ourCause.getClass().getName());
            sb.append(":");
            sb.append(ourCause.getMessage());
            sb.append("\n");
            StackTraceElement[] ourStes = ourCause.getStackTrace();
            
            int n = stes.length - 1, m = ourStes.length - 1;
            while(m >= 0 && n >= 0 && ourStes[m].equals(stes[n])) {
                m--; n--;
            }
            for(int i = 0; i <= m; i ++) {
                StackTraceElement ste = ourStes[i];
                if(ste.getFileName() != null) {
                    sb.append("    ");
                    sb.append(ste.toString());
                    sb.append("\n");
                }
            }
            
        }
        
        return sb.toString();
    }
    
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static String formatTime(long time){
        return sdf.format(new Date(time));
    }
    
    public static String formatTime(Date date){
        return sdf.format(date);
    }
    
}
