package lotus.or.orm.pool;

import or.lotus.common.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class LotusConnection implements Connection {
    static final Logger log = LoggerFactory.getLogger(LotusConnection.class);
    LotusDataSource dataSource;
    Connection rawConnection;
    long lastUseTime;
    long id;

    public LotusConnection(LotusDataSource dataSource, Connection rawConnection) {
        this.dataSource = dataSource;
        this.rawConnection = rawConnection;
        this.lastUseTime = System.currentTimeMillis();
        id = lastUseTime;
    }

    public boolean heartbeatTest() {
        int mill = dataSource.getConfig().getHeartbeatFreqSecs() * 1000;

        /*log.info("上次心跳: {}, 间隔: {}s, 当前时间: {}",
                Format.formatTime(lastUseTime),
                dataSource.getConfig().getHeartbeatFreqSecs(),
                Format.formatTime(System.currentTimeMillis())
                );*/

        if(lastUseTime + mill < System.currentTimeMillis()) {
            try {
                lastUseTime = System.currentTimeMillis();
                //log.info("心跳");
                return isValid(dataSource.getConfig().getHeartbeatTimeoutSeconds());
            } catch (SQLException e) {
                return false;
            }
        }
        return true;
    }

    public void closeRawConnection() {
        try {
            rawConnection.close();
        } catch (SQLException e) {

        }
    }

    public void use() {
        lastUseTime = System.currentTimeMillis();
    }

    public long getId() {
       return id;
    }

    @Override
    public void close() {
        use();
        dataSource.closeConnection(this);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return rawConnection.isClosed();
    }


    @Override
    public Statement createStatement() throws SQLException {
        return rawConnection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return rawConnection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return rawConnection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return rawConnection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        rawConnection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return rawConnection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        rawConnection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        rawConnection.rollback();
    }


    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return rawConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        rawConnection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return rawConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        rawConnection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return rawConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        rawConnection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return rawConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return rawConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        rawConnection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return rawConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return rawConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return rawConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return rawConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        rawConnection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        rawConnection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return rawConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return rawConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return rawConnection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        rawConnection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        rawConnection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return rawConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return rawConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return rawConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return rawConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return rawConnection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return rawConnection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return rawConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return rawConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return rawConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return rawConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return rawConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        rawConnection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        rawConnection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return rawConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return rawConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return rawConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return rawConnection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        rawConnection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return rawConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        rawConnection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        rawConnection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return rawConnection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return rawConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return rawConnection.isWrapperFor(iface);
    }
}
