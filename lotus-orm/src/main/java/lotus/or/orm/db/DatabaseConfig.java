package lotus.or.orm.db;

public class DatabaseConfig {
    public String url;
    public String username;
    public String password;
    public String driverName;
    public int maxConnection = 100;
    public int minConnection = 10;
    public String heartbeatSql;
    public int heartbeatFreqSecs = 30;
    public int heartbeatTimeoutSeconds = 30;

    public DatabaseConfig() {
    }

    public DatabaseConfig(String url, String username, String password) {
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
}
