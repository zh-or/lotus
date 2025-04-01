package or.lotus.v8.support;

import java.io.File;
import java.sql.DriverManager;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteOpenMode;


/**
 * sqlite
 * 如查询较多, 请创建索引,否则慢如狗.
 * @author or
 */
public class SqliteHelper implements AutoCloseable {
    private         ConcurrentHashMap<String, SqliteObject> jsSqliteObjects      =   null;
    private static  Object                                  sgLock               =   new Object();
    private static  SqliteHelper                            instance             =   null;

    public static SqliteHelper getInstance() {
        if(instance == null) {
            synchronized (sgLock) {
                if(instance == null) {
                    instance = new SqliteHelper();
                }
            }
        }
        return instance;
    }

    public SqliteHelper() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.jsSqliteObjects = new ConcurrentHashMap<String, SqliteObject>();
    }

    public SqliteObject open(String path, String name, SynchronousMode synMode, SQLiteOpenMode ...openMode) throws Exception {
        SqliteObject obj = jsSqliteObjects.get(name);

        boolean isNewDb = false;
        if(obj == null) {
            File dir = new File(path);
            if(!dir.exists()){
                dir.mkdirs();
            }
            File db = new File(path, name);
            if(!db.exists()){
                db.createNewFile();
                isNewDb = true;
            }

            SQLiteConfig connConfig = new SQLiteConfig();
            //connConfig.setOpenMode(SQLiteOpenMode.READWRITE);
            //https://www.sqlite.org/threadsafe.html
            //FULLMUTEX 是串行模式 串行模式可以单个链接多个线程用
            //NOMUTEX 是多线程模式 多线程模式不能单个链接多个线程用
            //SQLiteOpenMode.NOMUTEX
            for(SQLiteOpenMode mode : openMode) {
                connConfig.setOpenMode(mode);
            }

            connConfig.setSynchronous(synMode);//SynchronousMode.OFF
            connConfig.setBusyTimeout(10000);

            obj = new SqliteObject(
                    path,
                    name,
                    (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath(), connConfig.toProperties()),
                    db,
                    this
            );

            obj.setNewDb(isNewDb);
            jsSqliteObjects.put(name, obj);
        }
        return obj;
    }

    public SqliteObject getSqliteObject(String name) {
        return jsSqliteObjects.get(name);
    }

    public void removeSqliteObject(String name) {
        jsSqliteObjects.remove(name);
    }

    @Override
    public void close() {
        Collection<SqliteObject> objs = jsSqliteObjects.values();
        for(SqliteObject obj : objs) {
            obj.close();
        }
        jsSqliteObjects.clear();
    }
}
