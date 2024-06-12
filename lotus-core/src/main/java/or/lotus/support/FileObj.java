package or.lotus.support;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**java对象序列化存储到文件*/
public class FileObj<T> {
    private T obj;
    private String path;
    private GetFilter<T> filter;

    public FileObj(String path, GetFilter<T> filter) {
        this.path = path;
        ObjectInputStream oi = null;
        try {
            this.filter = filter;
            Path p = Paths.get(path);
            if (Files.exists(p)) {
                oi = new ObjectInputStream(new FileInputStream(p.toFile()));
                obj = (T) oi.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (oi != null) {
                    oi.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public T getObj() {
        if(filter != null) {
            T tmp = filter.get(obj);
            if(tmp != obj) {
                setObj(tmp);
            }
        }

        return obj;
    }

    public void setObj(T t) {
        obj = t;
        ObjectOutputStream oi = null;
        try {
            Path p = Paths.get(path);
            Files.deleteIfExists(p);
            oi = new ObjectOutputStream(new FileOutputStream(p.toFile()));
            oi.writeObject(obj);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (oi != null) {
                    oi.close();
                }
            } catch (IOException e) {
            }
        }
    }

   public interface GetFilter<T> {
        public T get(T t);
   }
}
