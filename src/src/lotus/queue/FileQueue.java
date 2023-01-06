package lotus.queue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

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

    public void close() {
        try {
            wait.notifyAll();
            raf.close();
        } catch (IOException e) {
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

    public String pollAndWait(long timeout) throws IOException {
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

    public synchronized void push(String line) throws IOException {
        fileLen = raf.length();
        raf.seek(fileLen);
        raf.write(line.getBytes(charset));
        synchronized(wait) {
            wait.notify();
        }
    }
}
