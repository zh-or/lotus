package or.lotus.core.queue;

import or.lotus.core.common.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**一行一条数据, 默认最大存储10GB*/
public class FileQueue implements AutoCloseable {
    private Object              wait            =   new Object();
    private Charset             charset         =   Charset.forName("utf-8");
    private long                readPos;
    private long                fileLen;
    private RandomAccessFile    raf;

    private long                autoGcSize     = Utils.formatSize("10GB");

    public FileQueue(String path) throws IOException {
        this(new File(path));
    }

    public FileQueue(File f) throws IOException {
        raf = new RandomAccessFile(f, "rw");
        readPos = raf.length();
    }

    public void setAutoGcSize(long size) {
        this.autoGcSize = size;
    }

    /** 设置读写位置为文件起始位置, 并设置文件长度为0 */
    public synchronized void gc() throws IOException {
        fileLen = raf.length();
        raf.seek(fileLen);
        raf.setLength(0);
        fileLen = 0;
    }

    @Override
    public void close() {
        try {
            synchronized (wait) {
                wait.notifyAll();
            }
            raf.close();
        } catch (Exception e) {
        }
    }

    public void setReadPos(long pos) {
        readPos = pos;
    }

    /**此方法会一直等待数据*/
    private synchronized String poll()  {
        try {
            raf.seek(readPos);
            String line = raf.readLine();
            readPos = raf.getFilePointer();
            return line;
        } catch(Exception e) {}
        return null;
    }

    public String pollAndWait(long timeout) throws Exception {
        if(readPos + 1 >= fileLen) {
            try {
                synchronized(wait) {
                    wait.wait(timeout);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return poll();
    }

    public synchronized void push(String line) throws Exception {
        fileLen = raf.length();

        if(fileLen + line.length() >= autoGcSize) {
            gc();
        }

        raf.seek(fileLen);
        raf.write(line.getBytes(charset));
        fileLen = raf.length();
        synchronized(wait) {
            wait.notify();
        }
    }
}
