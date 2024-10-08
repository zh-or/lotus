package or.lotus.orm.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements AutoCloseable, InvocationHandler {
    static final Logger log = LoggerFactory.getLogger(Transaction.class);
    Connection connection;
    Connection proxyConnection;
    Database db;
    boolean isCommit = false;
    boolean rawIsAutoCommit = false;

    public Transaction(Database db) throws SQLException {
        this.db = db;
        connection = db.getConnection();
        rawIsAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        Class<?> clazz = connection.getClass();
        proxyConnection = (Connection) Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), this);
        db.transactionConnection.set(proxyConnection);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public void commit() throws SQLException {
        connection.commit();
        isCommit = true;
    }

    public void rollback() throws SQLException {
        if(isCommit) {
            throw new SQLException("当前事务已经提交成功了, 无法回滚!!!");
        }
        connection.rollback();
    }


    @Override
    public void close() {
        try {
            if(!isCommit) {//未提交事物执行一次回滚操作
                connection.rollback();
            }
        } catch (SQLException e) {
            log.debug("自动回滚异常:", e);
        }
        db.transactionConnection.remove();
        try {
            connection.setAutoCommit(rawIsAutoCommit);
        } catch (SQLException e) {
            log.error("事物还原AutoCommit出错 值={}", rawIsAutoCommit, e);
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.error("事物关闭原始连接出错:", e);
        }
    }

    public Connection getConnection() {
        return proxyConnection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if("close".equals(method.getName())) {
            //开启事物时忽略close方法
            return null;
        }
        //其他调用直接传输给原始连接
        return method.invoke(connection, args);
    }
}
