package lotus.or.orm.db;

import lotus.or.orm.pool.LotusConnection;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements Closeable {
    Connection connection;
    Database db;
    boolean isCommit = false;

    public Transaction(Database db) throws SQLException {
        this.db = db;
        connection = db.getConnection();
        connection.setAutoCommit(false);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }


    @Override
    public void close() {
        try {
            if(!isCommit) {
                connection.rollback();
            }
            db.endTransaction();
            connection.close();
        } catch (SQLException e) {}
    }

    public Connection getConnection() {
        return connection;
    }
}
