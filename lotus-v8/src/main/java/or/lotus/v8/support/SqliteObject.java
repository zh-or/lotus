package or.lotus.v8.support;

import java.io.File;
import java.sql.SQLException;

import org.sqlite.SQLiteConnection;


public class SqliteObject {

    private SQLiteConnection    conn             =   null;
    private String              name             =   null;
    private String              path             =   null;
    private File                dbFile           =   null;
    private SqliteHelper        context          =   null;
    private boolean             isNewDb          =   false;

    public SqliteObject(String path, String name, SQLiteConnection conn, File dbFile, SqliteHelper context) {
        this.name = name;
        this.path = path;
        this.conn = conn;
        this.dbFile =  dbFile;
        this.context = context;
    }


    public boolean isNewDb() {
        return isNewDb;
    }

    public void setNewDb(boolean isNewDb) {
        this.isNewDb = isNewDb;
    }

    /**
     * 关闭当前连接
     */
    public void close() {
        if(conn != null) {
            try {
                conn.close();
                context.removeSqliteObject(name);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public SQLiteConnection getConn() {
        return conn;
    }


    public String getName() {
        return name;
    }


    public String getPath() {
        return path;
    }


    public File getDbFile() {
        return dbFile;
    }


    public SqliteHelper getContext() {
        return context;
    }


}
