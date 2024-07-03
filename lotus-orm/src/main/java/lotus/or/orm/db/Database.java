package lotus.or.orm.db;

import lotus.or.orm.pool.DataSourceConfig;
import lotus.or.orm.pool.LotusConnection;
import lotus.or.orm.pool.LotusDataSource;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    ConcurrentHashMap<String, DataSource> dataSources = null;
    String name = "default";

    private DataSourceConfig config = null;

    ThreadLocal<Connection> transactionConnection = new ThreadLocal<Connection>();

    public Database() {
    }

    public Database forName(String name) {
        Database db = new Database();
        db.name = name;
        db.dataSources = dataSources;
        return db;
    }

    public String getName() {
        return name;
    }

    /**name for default*/
    public void registerDataSource(DataSource dataSource) {
        registerDataSource("default", dataSource);
    }
    public void registerDataSource(String name, DataSource dataSource) {
        if(dataSources == null) {
            dataSources = new ConcurrentHashMap<String, DataSource>();
        }
        dataSources.put(name, dataSource);
    }

    /**设置后将优于 LotusDataSource 的配置 */
    public void setConfig(DataSourceConfig config) {
        this.config = config;
    }

    public DataSourceConfig getConfig() {
        if(config != null) {
            return config;
        }
        try {
            DataSource ds = getDataSource();
            if(ds instanceof LotusDataSource) {
                return ((LotusDataSource) ds).getConfig();
            }
        } catch(SQLException e) {
            //ignore exception
        }
        return new DataSourceConfig();
    }

    public DataSource getDataSource() throws SQLException {
        DataSource ds = dataSources.get(name);
        if(ds == null) {
            throw new SQLException("not found datasource");
        }
        return ds;
    }

    public Connection getConnection() throws SQLException {
        DataSource dataSource = getDataSource();
        Connection connection = transactionConnection.get();
        if(connection == null) {
            connection = dataSource.getConnection();
        }
        return connection;
    }

    public Transaction beginTransaction() throws SQLException {
        Transaction transaction = new Transaction(this);
        transactionConnection.set(transaction.getConnection());

        return transaction;
    }

    public void endTransaction() throws SQLException {
        transactionConnection.remove();
    }

    public int execSqlUpdate(String sql) {
        return 0;
    }

    public <T> DatabaseExecutor<T> selectDto(Class<T> clazz, String sql)  {
        DatabaseExecutor exec = new DatabaseExecutor(this, "select", clazz, null);
        exec.getSqlBuilder().setSql(sql);
        exec.fieldsFromObj();
        return exec;
    }

    public <T> T select(Class<T> clazz, int id)  {
        DatabaseExecutor exec = new DatabaseExecutor(this, "select", clazz, null);
        exec.fieldsFromObj();
        exec.whereEq("id", String.valueOf(id));
        return (T) exec.findOne();
    }

    public <T> DatabaseExecutor<T> select(Class<T> clazz)  {
        DatabaseExecutor exec = new DatabaseExecutor(this, "select", clazz, null);

        exec.fieldsFromObj();
        return exec;
    }

    public int delete(Object obj) {
        DatabaseExecutor exec = new DatabaseExecutor(this, "delete", obj.getClass(), obj);
        return exec.exec();
    }

    public int updateAll(List<Object> obj) {
        if(obj == null && obj.size() == 0) {
            return 0;
        }
        Object first = obj.get(0);
        Class<?> clazz = first.getClass();
        DatabaseExecutor exec = new DatabaseExecutor(this, "update", clazz, obj);
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);
        for(Field f : fields) {
            fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(f.getName()));
        }
        exec.fieldList(fieldNames);
        exec.useDefaultField(getConfig().getPrimaryKeyName());
        return exec.exec();
    }

    public int update(Object obj) {
        List<Object> objs = new ArrayList<>(1);
        objs.add(obj);
        return updateAll(objs);
    }

    public int insertAll(List<Object> obj) {
        if(obj == null && obj.size() == 0) {
            return 0;
        }
        Object first = obj.get(0);
        Class<?> clazz = first.getClass();
        DatabaseExecutor exec = new DatabaseExecutor(this, "insert", clazz, obj);
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);
        for(Field f : fields) {
            fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(f.getName()));
        }
        exec.fieldList(fieldNames);
        exec.useDefaultField(getConfig().getPrimaryKeyName());
        return exec.exec();
    }

    public int insert(Object obj) {
        List<Object> objs = new ArrayList<>(1);
        objs.add(obj);
        return insertAll(objs);
    }
}
