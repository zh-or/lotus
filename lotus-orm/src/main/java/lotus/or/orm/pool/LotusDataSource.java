package lotus.or.orm.pool;


import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LotusDataSource implements DataSource {
    static final Logger log = LoggerFactory.getLogger(LotusDataSource.class);
    LinkedBlockingQueue<LotusConnection> pool = new LinkedBlockingQueue<>();
    HashSet<String> driverLoadState = new HashSet<>();

    protected AtomicInteger poolSize = new AtomicInteger(0);
    protected DataSourceConfig config;
    protected Timer timer;

    public LotusDataSource() { }

    public String toString() {
        return String.format("i: %d, size: %d", poolSize.get(), pool.size());
    }

    public void setConfig(DataSourceConfig config) throws SQLException {
        this.config = config;
        this.initDataSource();
    }

    public void closeConnection(LotusConnection conn) {
        try {
            if(conn.isValid(1)) {
                pool.add(conn);
                return;
            }
        } catch (Exception e) {
        }
        poolSize.decrementAndGet();
        conn.closeRawConnection();
    }

    protected void initDataSource() throws SQLException {
        do {
            LotusConnection conn = pool.poll();
            if(conn == null) {
                break;
            }
            conn.close();
        } while(true);

        pool.clear();

        for(int i = 0; i < config.getMinConnection(); i++) {
            LotusConnection conn = (LotusConnection) getConnection();
            if(conn == null) {
                throw new SQLException("请检查配置是否错误, 无法获取连接: {}", config.getUrl());
            }
            pool.add(conn);
        }

        if(timer != null) {
            timer.cancel();
        }
        timer = new Timer("lotus data source heartbeat");
        timer.schedule(timerTask, config.getHeartbeatFreqSecs() * 1000, config.getHeartbeatFreqSecs() * 1000);


    }

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            for(int i = 0; i < poolSize.get(); i ++) {
                try {
                    LotusConnection conn = (LotusConnection) getConnection();
                    if(conn == null) {
                        break;
                    }

                    if(poolSize.get() <= config.getMinConnection() && conn.heartbeatTest()) {
                        pool.add(conn);
                    } else {
                        poolSize.decrementAndGet();
                        conn.closeRawConnection();
                    }
                } catch (Exception e) {
                    log.error("heartbeat test error", e);
                }
                //调低线程优先级
                Utils.SLEEP(1);
            }

        }
    };

    public DataSourceConfig getConfig() {
        return config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(config.getUsername(), config.getPassword());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        LotusConnection connection = pool.poll();
        boolean needCheck = false;
        if(connection == null) {
            if(poolSize.get() >= config.getMaxConnection()) {
                try {
                    connection = pool.poll(config.getLoginTimeout(), TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.error("数据库连接池连接耗尽, 等待返回连接失败");
                    return null;
                }
                needCheck = true;
            } else {
                try {
                    String driverName = config.getDriverName();
                    synchronized (driverLoadState) {
                        if(!driverLoadState.contains(driverName)) {
                            Class.forName(config.getDriverName());
                            driverLoadState.add(driverName);
                        }
                    }
                    Connection tmp = DriverManager.getConnection(config.getUrl(), username, password);
                    connection = new LotusConnection(this, tmp);
                    poolSize.incrementAndGet();
                } catch (ClassNotFoundException e) {
                    log.error("驱动未找到:", e);
                }
            }
        }
        if(needCheck) {
            if(!connection.heartbeatTest()) {
                connection.closeRawConnection();
                poolSize.decrementAndGet();
                log.error("连接心跳超时, 间隔时间: {}ms, id:{}", System.currentTimeMillis() - connection.lastUseTime, connection.getId());
                return getConnection(username, password);
            }
        }
        connection.use();
        return connection;
    }


    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    protected PrintWriter out;
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return out;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.out = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        config.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return config.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return java.util.logging.Logger.getLogger("LotusDataSource");
    }
}
