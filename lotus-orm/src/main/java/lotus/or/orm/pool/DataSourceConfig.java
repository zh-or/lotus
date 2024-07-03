package lotus.or.orm.pool;

public class DataSourceConfig {
    public String url;
    public String username;
    public String password;
    public String driverName;
    public int maxConnection = 100;
    public int minConnection = 10;
    public int heartbeatFreqSecs = 30;//心跳检测间隔
    public int heartbeatTimeoutSeconds = 5;//心跳检测超时时间
    public int loginTimeout = 30;//获取连接超时时间 秒

    public String primaryKeyName = "id";//主键名字
    public boolean insertIgnoreNull = true;//插入时忽略空值
    public boolean updateIgnoreNull = true;//更新时忽略空值

    public DataSourceConfig() {
    }

    public DataSourceConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverName() {
        if(driverName == null) {
            if(url.startsWith("jdbc:mysql")) {
                return "com.mysql.jdbc.Driver";
            }
        }

        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public int getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(int maxConnection) {
        this.maxConnection = maxConnection;
    }

    public int getMinConnection() {
        return minConnection;
    }

    public void setMinConnection(int minConnection) {
        this.minConnection = minConnection;
    }

    public int getHeartbeatFreqSecs() {
        return heartbeatFreqSecs;
    }

    public void setHeartbeatFreqSecs(int heartbeatFreqSecs) {
        this.heartbeatFreqSecs = heartbeatFreqSecs;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public String getPrimaryKeyName() {
        return primaryKeyName;
    }

    public void setPrimaryKeyName(String primaryKeyName) {
        this.primaryKeyName = primaryKeyName;
    }

    public boolean isInsertIgnoreNull() {
        return insertIgnoreNull;
    }

    public void setInsertIgnoreNull(boolean insertIgnoreNull) {
        this.insertIgnoreNull = insertIgnoreNull;
    }

    public boolean isUpdateIgnoreNull() {
        return updateIgnoreNull;
    }

    public void setUpdateIgnoreNull(boolean updateIgnoreNull) {
        this.updateIgnoreNull = updateIgnoreNull;
    }
}
