package or.lotus.core.ip2reg;

import com.maxmind.db.CHMCache;
import com.maxmind.db.MaxMindDbParameter;
import or.lotus.core.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import com.maxmind.db.Reader;

public class IP2RegionByMMDB implements AutoCloseable {
    protected static Logger log = LoggerFactory.getLogger(IP2RegionByMMDB.class);
    /** 默认下载地址 */
    public static String dbDownloadUrl2 = "https://cdn.jsdelivr.net/npm/geolite2-city/GeoLite2-City.mmdb.gz";
    public static String dbDownloadUrl = "https://github.com/wp-statistics/GeoLite2-City/raw/refs/heads/master/GeoLite2-City.mmdb.gz";

    /**下载超时时间, 0为不超时*/
    public static int dbDownloadTimeout = 1000 * 60 * 60;
    public void updateDb(String savePath) {
        updateDb(dbDownloadUrl, savePath);
    }

    /** 下载gz时直接解压 */
    public void updateDb(String url, String savePath) {
        InputStream in = null;
        FileOutputStream fileOut = null;
        try {
            log.info("开始更新mmdb, url: {}, savePath: {}", dbDownloadUrl, savePath);
            URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(dbDownloadTimeout);
            conn.connect();
            in = conn.getInputStream();
            if(url.endsWith(".gz")) {
                //gz时直接用gzip解压
                in = new GZIPInputStream(in);
            }
            Files.deleteIfExists(Paths.get(savePath));
            fileOut = new FileOutputStream(savePath);
            byte[] buf = new byte[1024 * 4];
            int len;
            while((len = in.read(buf)) != -1) {
                fileOut.write(buf, 0, len);
            };
            fileOut.flush();
            log.info("更新mmdb数据库完成...");
        } catch (Exception e) {
            log.error("更新mmdb数据库失败:", e);
        } finally {
            Utils.closeable(in);
            Utils.closeable(fileOut);
        }
    }

    Reader dbReader = null;

    /**
     * @param useCache 使用缓存时需要大约 2MB 内存
     * */
    public void loadDb(String dbPath, boolean useCache) throws IOException {
        dbReader = useCache ? new Reader(new File(dbPath), new CHMCache()) : new Reader(new File(dbPath));
        /*Map<String, Object> o = dbReader.get(InetAddress.getByName("24.24.24.24"), Map.class);
        log.info("test");*/
    }

    public static String defaultLang = "zh-CN";

    public MMDBInfo get(String ip) throws IOException {
        return get(InetAddress.getByName(ip), defaultLang);
    }

    public MMDBInfo get(String ip, String lang) throws IOException {
        return get(InetAddress.getByName(ip), lang);
    }

    public MMDBInfo get(InetAddress addr, String lang) throws IOException {
        MMDBInfo info = new MMDBInfo();
        Map<String, Object> map = dbReader.get(addr, Map.class);
        Field[] fields = info.getClass().getFields();
        try {
            for(Field f : fields) {
                MaxMindDbParameter mmp = f.getAnnotation(MaxMindDbParameter.class);
                if(mmp != null) {
                    String key = mmp.name();

                    key = key.replaceAll(defaultLang, lang);
                    Object val = getValueByMap(map, key.split("\\."), 0);

                    if(val == null) {//有可能没有该语言的
                        key = key.replaceAll(lang, "en");
                        val = getValueByMap(map, key.split("\\."), 0);
                    }

                    f.setAccessible(true);
                    f.set(info, val);
                }
            }
            return  info;
        } catch (IllegalAccessException e) {
            log.error("反射取值出错:", e);
        }
        return null;
    }

    public Object getValueByMap(Map map, String[] keys, int index) {
        if(keys.length > index + 1) {//不是最后一个
            Object child = map.get(keys[index]);
            if(child == null) {
                //
                return null;
            }
            if(child instanceof List) {
                child = ((List) child).get(0);
            }
            if(!(child instanceof Map)) {
                throw new RuntimeException("结构不对 -> " + keys[index]);
            }

            return getValueByMap((Map) child, keys, index + 1);
        }
        return map.get(keys[index]);
    }

    @Override
    public void close() throws Exception {
        Utils.closeable(dbReader);
        dbReader = null;//cache 用的 MappedByteBuffer 会导致关闭后文件不释放, 可尝试手动gc
    }

    public static void main(String[] args) throws IOException {
        IP2RegionByMMDB ip2Region = new IP2RegionByMMDB();
        ip2Region.loadDb("./test/GeoLite2-City.mmdb", true);
        MMDBInfo info = ip2Region.get("240e:331:6f9:3f00:545d:ebfb:31a3:f28");
        log.info("test:" + info);
    }
}
