package lotus.or.orm.db;

import lotus.or.orm.pool.LotusConnection;
import lotus.or.orm.pool.LotusDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    ConcurrentHashMap<String, DataSource> dataSources = null;
    String name = "default";

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
        DatabaseExecutor exec = new DatabaseExecutor(this, "select", clazz);
        exec.getSqlBuilder().setSql(sql);
        exec.fieldsFromObj();
        return exec;
    }

    public <T> DatabaseExecutor<T> select(Class<T> clazz)  {
        DatabaseExecutor exec = new DatabaseExecutor(this, "select", clazz);

        exec.fieldsFromObj();
        return exec;
    }

    public <T> DatabaseExecutor<T> delete(Object obj) {
        DatabaseExecutor exec = new DatabaseExecutor(this, "delete", obj.getClass());

        return exec;
    }

    public <T> DatabaseExecutor<T> updateAll(Object obj) {
        DatabaseExecutor exec = new DatabaseExecutor(this, "update", obj.getClass());

        exec.fieldsFromObj();
        return exec;
    }

    public <T> DatabaseExecutor<T> update(Object obj) {
        DatabaseExecutor exec = new DatabaseExecutor(this, "update", obj.getClass());

        exec.fieldsFromObj();
        return exec;
    }

    public int insertAll(List<Object> obj) {
        DatabaseExecutor exec = new DatabaseExecutor(this, "insert", obj.getClass());

        exec.fieldsFromObj();
        return exec.exec();
    }

    public int insert(Object obj) {
        DatabaseExecutor exec = new DatabaseExecutor(this, "insert", obj.getClass());
        exec.fieldsFromObj();
        return exec.exec();
    }
}
