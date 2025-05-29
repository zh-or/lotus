package or.lotus.core.files;

import or.lotus.core.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/** 本地kv存储 */
public class LocalFileManager {
    protected static Logger log = LoggerFactory.getLogger(LocalFileManager.class);
    protected String basePath;
    protected long maxLocalSize;
    protected AtomicLong nowSize;
    protected String charset;

    public LocalFileManager(String basePath, String charset) {
        this(basePath, Utils.formatSize("10GB"), charset);
    }

    public LocalFileManager(String basePath, long maxLocalSize, String charset) {
        this.basePath = basePath;
        this.maxLocalSize = maxLocalSize;
        this.nowSize = new AtomicLong(0l);
        this.charset = charset;
        refreshSize();

        log.info("文件管理器初始化完成, 当前大小: {}, 根目录: {}", new FileSize(nowSize.get()), basePath);
    }

    public boolean isFullSize() {
        return nowSize.get() > maxLocalSize;
    }

    public void refreshSize() {
        try {
            nowSize.set(0);
            Files.walkFileTree(Paths.get(basePath), new FileVisitor() {

                @Override
                public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
                    nowSize.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            log.error("刷新文件大小失败:", e);
        }
    }

    /**获取字符串*/
    public String getString(String key) {
        byte[] bs = getBytes(key);
        if(bs != null) {
            return new String(bs, Charset.forName(charset));
        }
        return null;
    }

    /**获取字节数组*/
    public byte[] getBytes(String key) {
        try {
            Path p = getKeyPath(key);
            if(Files.exists(p)) {
                return Files.readAllBytes(p);
            }
        } catch (Exception e) {
            log.error("读取key的值失败:", e);
        }
        return null;
    }

    /**保存字节数组*/
    public void put(String key, byte[] data) throws IOException, NoSuchAlgorithmException {
        Utils.assets(data == null, "data 不能为空");

        //k v 存储
        Path p = getKeyPath(key);
        //Files.deleteIfExists(p);
        //不使用删除, 使用TRUNCATE_EXISTING可以复写该文件
        Files.write(p, data, StandardOpenOption.TRUNCATE_EXISTING,  StandardOpenOption.WRITE, StandardOpenOption.CREATE);

        nowSize.addAndGet(data.length);
    }

    /**保存字符串*/
    public void put(String key, String data) throws IOException, NoSuchAlgorithmException {
        put(key, data.getBytes(charset));
    }

    public void append(String key, byte[] data) throws IOException, NoSuchAlgorithmException {
        Utils.assets(data == null, "data 不能为空");

        //k v 存储
        Path p = getKeyPath(key);
        Files.write(p, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);

        nowSize.addAndGet(data.length);
    }

    public void append(String key, String data) throws IOException, NoSuchAlgorithmException {
        append(key, data.getBytes(charset));
    }


    /**根据key移除文件*/
    public boolean remove(String key) {
        try {
            Path p = getKeyPath(key);
            if(Files.exists(p)) {
                nowSize.addAndGet(-Files.size(p));
                Files.delete(p);
            }
            return true;
        } catch (Exception e) {
            log.error("删除key的值失败:", e);
        }
        return false;
    }

    public File getFile(String key) {
        try {
            Path p = getKeyPath(key);
            if(Files.exists(p)) {
                return p.toFile();
            }
        } catch (Exception e) {
            log.error("从key获取文件失败:", e);
        }
        return null;
    }

    public void putFile(String key, File file) {
        try {
            Path p = getKeyPath(key);
            if(Files.exists(p)) {
                nowSize.addAndGet(-Files.size(p));
                Files.delete(p);
            }
            nowSize.addAndGet(file.length());
            Files.copy(file.toPath(), p);
        } catch (Exception e) {
            log.error("保存文件失败:", e);
        }
    }

    /**生成kv存储的文件路径*/
    public Path getKeyPath(String key) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Utils.assets(Utils.CheckNull(key), "key 不能为空");
        //路径的特殊字符需要处理掉
        key = key.replaceAll(":", "_");

        String sha1 = Utils.EnCode(key, Utils.EN_TYPE_SHA1);
        String first = sha1.substring(0, 4);
        String second = sha1.substring(4, 8);
        Path p = Paths.get(basePath, first, second);

        if(!Files.exists(p)) {
            p.toFile().mkdirs();
        }

        return p.resolve(key);
    }

}
