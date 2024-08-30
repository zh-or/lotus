package or.lotus.core.ip2reg;

import org.lionsoul.ip2region.xdb.Searcher;

import java.io.IOException;

/**
 * 库地址: https://gitee.com/lionsoul/ip2region
 */
public class IP2Region implements AutoCloseable {

    Searcher searcher;
    static final String NULL_STR = "";

    public IP2Region(String dbPath) throws IOException {
        searcher = Searcher.newWithVectorIndex(dbPath, Searcher.loadVectorIndexFromFile(dbPath));
    }

    public synchronized void reloadDb(String dbPath) throws IOException {
        if(searcher != null) {
            searcher.close();
            searcher = null;
        }
        searcher = Searcher.newWithVectorIndex(dbPath, Searcher.loadVectorIndexFromFile(dbPath));
    }

    public synchronized String searchStr(String ip) {
        try {
            return searcher.search(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NULL_STR;
    }

    public IpInfo search(String ip) {
        String region = searchStr(ip);
        if (region == null) {
            return null;
        }
        IpInfo ipInfo = new IpInfo(region);
        return ipInfo;
    }

    @Override
    public void close() throws IOException {
        searcher.close();
    }
}
