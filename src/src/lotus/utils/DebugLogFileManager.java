package lotus.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class DebugLogFileManager {
    private static Object lock = new Object();
    private static DebugLogFileManager instance = null;
    private static String dirPath = "./debug-log/";

    private ConcurrentHashMap<String, RandomAccessFile> files;

    private DebugLogFileManager() {
        files = new ConcurrentHashMap<>();
    }

    public static DebugLogFileManager getInstance() {
        if(instance == null) {
            synchronized (lock) {
                if(instance == null) {
                    Path dir = Paths.get(dirPath);
                    if(Files.notExists(dir)) {
                        try {
                            Files.createDirectory(dir);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    instance = new DebugLogFileManager();
                }
            }
        }
        return instance;
    }

    public void free() {
        files.forEach((k, v) -> {
            try {
                v.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void append(String name, ByteBuffer data) {
        data.mark();
        byte[] buff = new byte[data.remaining()];
        data.get(buff);
        data.reset();
        append(name, data, false);
    }

    public void append(String name, ByteBuffer data, boolean hex) {
        if(data.hasArray()) {
            byte[] arr = data.array();
            int pos = data.position();
            append(name,"===========================================================");
            if(hex) {
                String hexStr = Utils.byte2hex(arr, pos);
                append(name, hexStr);
            }
            try {
                append(name, new String(arr, 0, pos, "utf-8"));
                append(name,"===========================================================");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public void append(String name, String data) {
        RandomAccessFile raf;
        synchronized (files) {
            raf = files.get(name);
            if(raf == null) {
                try {
                    raf = new RandomAccessFile(Paths.get(dirPath, name + ".log").toFile(), "rw");
                    files.put(name, raf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (raf) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date = sdf.format(new Date());
                raf.writeBytes(date);
                raf.writeBytes(":");
                raf.writeBytes(data + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
