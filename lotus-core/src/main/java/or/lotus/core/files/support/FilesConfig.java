package or.lotus.core.files.support;

import or.lotus.core.common.Utils;

import java.nio.charset.Charset;

public class FilesConfig {
    public Charset charset = Charset.forName("utf-8");
    public int maxConnectNum = 20;//client 最大连接数
    public int minConnectNum = 10;
    public String token;
    public String host;
    public int port;
    public String localPath;
    public String localMaxSize;//TB, GB, MB, KB = default
    public long maxSplitSize;//分片大小

    public FilesConfig(String token, String host, int port, String localPath, String localMaxSize) {
        this.token = token;
        this.host = host;
        this.port = port;
        this.localPath = localPath;
        this.localMaxSize = localMaxSize;
        this.maxSplitSize = Utils.formatSize("5MB");
    }

    /**1TB, 1GB, 1MB, 1KB*/
    public void setMaxSplitSize(String maxSplitSize) {
        this.maxSplitSize = Utils.formatSize(maxSplitSize);
    }

    public void setMaxConnectNum(int maxConnectNum) {
        this.maxConnectNum = maxConnectNum;
    }

    public void setMinConnectNum(int minConnectNum) {
        this.minConnectNum = minConnectNum;
    }

    public long getLocalMaxSize() {
        return Utils.formatSize(localMaxSize);
    }

    public void setCharset(String name) {
        this.charset = Charset.forName(name);
    }
}
