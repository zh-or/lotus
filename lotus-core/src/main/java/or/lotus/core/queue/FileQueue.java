package or.lotus.core.queue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**数据存储到文件的队列*/
public class FileQueue {
    private Object              wait            =   new Object();
    private Charset             charset         =   Charset.forName("utf-8");
    private long                readPos;
    private long                fileLen;
    private RandomAccessFile    raf;

    public FileQueue(String path) throws IOException {
        this(new File(path));
    }

    public FileQueue(File f) throws IOException {
        raf = new RandomAccessFile(f, "rw");
        readPos = raf.length();
    }

    public void gc() {
        //to do
    }

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
            }
        }
        return poll();
    }

    public synchronized void push(String line) throws Exception {
        fileLen = raf.length();
        raf.seek(fileLen);
        raf.write(line.getBytes(charset));
        fileLen = raf.length();
        synchronized(wait) {
            wait.notify();
        }
    }
}
