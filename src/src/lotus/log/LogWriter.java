package lotus.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LogWriter {
    private FileOutputStream                      logFileOut      = null;
    private static Object                         lockLogFile     = new Object();
    private static LinkedBlockingQueue<String>    qLogs           = null;
    private String                                dir             = "./log";

    public LogWriter() {
        qLogs = new LinkedBlockingQueue<>(120);
        new Thread(new Runnable() {

            @Override
            public void run() {
                while(true) {
                    try {
                        String log = null;
                        try {
                            log = qLogs.poll(500, TimeUnit.MILLISECONDS);
                            if(log != null) {
                                reInitLogFile();
                                logFileOut.write(log.getBytes());
                                logFileOut.write('\n');
                                logFileOut.flush();
                            }
                        }catch(Exception e) {

                        }

                    }catch(Exception e) {
                        e.printStackTrace();
                        //_log.error("写出日志到文件出错: %s", Format.formatException(e));
                    }
                }
            }
        }, "log file write thread").start();
    }

    private int nowDay = -1;

    public void reInitLogFile(){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int tmpDay = cal.get(Calendar.DAY_OF_YEAR);
        if(nowDay != tmpDay) {
            nowDay = tmpDay;
            synchronized (lockLogFile) {
                if(logFileOut != null){
                    try {
                        logFileOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //_log.error("关闭日志文件失败, %s", Format.formatException(e));
                    }
                }
                File fDir = new File(dir);
                if(!fDir.exists()){
                    fDir.mkdirs();
                }
                File tmp = new File(
                        fDir,
                    String.format(
                        "%d-%d-%d.log",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                );
                if(!tmp.exists()){
                    try {
                        tmp.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //_log.error("创建日志文件失败, %s", Format.formatException(e));
                    }
                }
                if(tmp.exists()){
                    try {
                        logFileOut = new FileOutputStream(tmp, true);
                    } catch (FileNotFoundException e) {
                        //_log.error("打开日志文件失败, %s", Format.formatException(e));
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void write(String line) {
        qLogs.add(line);
    }
}
