package or.lotus.v8.lib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import or.lotus.core.common.Utils;
import or.lotus.v8.Message;
import or.lotus.v8.V8Context;
import or.lotus.v8.support.JavaLibBase;
import or.lotus.v8.support.SqliteHelper;
import or.lotus.v8.support.SqliteObject;
import or.lotus.v8.support.SqliteRSObject;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

public class V8SqliteLib extends JavaLibBase {
    private V8Context base       = null;
    private V8           runtime    = null;
    private SqliteHelper helper     = null;

    protected final int EXEC_INSERT = V8Context.getMessageId();
    protected final int EXEC_QUERY  = V8Context.getMessageId();
    protected final int EXEC_UPDATE = V8Context.getMessageId();
    protected final int EXEC_EXEC   = V8Context.getMessageId();

    @Override
    public void onInit(V8Context v8b) {
        helper = SqliteHelper.getInstance();
        base = v8b;
        runtime = v8b.getRuntimer();
        V8Object db = new V8Object(runtime);
        db.registerJavaMethod(this, "open", "openSqlite", new Class<?>[] { String.class, String.class, String.class, String[].class });
        runtime.add("db", db);
    }

    @Override
    public void onQuit() {

    }

    @Override
    public void onDestroy() {
        //helper.close();
    }

    @Override
    public boolean MessageLoop(Message msg) {
        int type = msg.getType();
        if(type != EXEC_EXEC && type != EXEC_INSERT && type != EXEC_QUERY && type != EXEC_UPDATE) {
            return false;
        }
        V8Object   receiver   = null;
        V8Function fun        = null;
        V8Object   res        = null;
        V8Array    parameters = null;
        try {
            receiver = (V8Object) msg.getMsg();
            fun = (V8Function) msg.getAttr1();
            int state = (int) msg.getAttr2();

            base.removeSyncObj(receiver);
            base.removeSyncObj(fun);

            if(type == EXEC_INSERT) {
                parameters = new V8Array(runtime);
                parameters.push(state);
                res = (V8Object) msg.getAttr3();//用于内存释放
                fun.call(receiver, parameters);
                return true;
            }

            if(type == EXEC_QUERY) {
                @SuppressWarnings("unchecked")
                ArrayList<SqliteRSObject[]> data =  (ArrayList<SqliteRSObject[]>) msg.getAttr3();

                res = new V8Array(runtime);
                for(SqliteRSObject[] row : data) {
                    V8Object obj = new V8Object(runtime);
                    for(SqliteRSObject column : row) {
                        Class<?> cType = column.value.getClass();
                        if (cType == String.class)
                            obj.add(column.name, (String) column.value);
                        else if (cType == Boolean.class || cType == boolean.class)
                            obj.add(column.name, (Boolean)column.value);
                        else if (cType == Long.class || cType == long.class)
                            obj.add(column.name, String.valueOf(column.value));
                        else if (cType == Integer.class || cType == int.class)
                            obj.add(column.name, (Integer)column.value);
                        else if (cType == Short.class || cType == short.class)
                            obj.add(column.name, (Short)column.value);
                        else if (cType == Float.class || cType == float.class)
                            obj.add(column.name, (Float) column.value);
                        else if (cType == Double.class || cType == double.class)
                            obj.add(column.name, (Double) column.value);
                    }
                    ((V8Array) res).push(obj);
                    obj.close();
                }

                parameters = new V8Array(runtime);
                parameters.push(state);
                parameters.push(res);//查询结果集
                fun.call(receiver, parameters);

                return true;
            }

            if (type == EXEC_UPDATE) {
                parameters = new V8Array(runtime);
                parameters.push(state);//影响行数
                fun.call(receiver, parameters);
                return true;
            }

            if (type == EXEC_EXEC) {
                parameters = new V8Array(runtime);
                parameters.push(state);//影响行数
                fun.call(receiver, parameters);
                return true;
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (receiver != null) {
                receiver.close();
            }
            if (fun != null) {
                fun.close();
            }
            if (res != null) {
                res.close();
            }
            if (parameters != null) {
                parameters.close();
            }
        }
        return false;
    }

    /*
    *
    *     READONLY(1),
    READWRITE(2),
    CREATE(4),
    DELETEONCLOSE(8),
    EXCLUSIVE(16),
    OPEN_URI(64),
    OPEN_MEMORY(128),
    MAIN_DB(256),
    TEMP_DB(512),
    TRANSIENT_DB(1024),
    MAIN_JOURNAL(2048),
    TEMP_JOURNAL(4096),
    SUBJOURNAL(8192),
    MASTER_JOURNAL(16384),
    NOMUTEX(32768),
    FULLMUTEX(65536),
    SHAREDCACHE(131072),
    PRIVATECACHE(262144);
    * */
    public V8Object open(String path, String name, String syncMode, String ...openMode) {
        try {
            if(Utils.CheckNull(syncMode)) {
                syncMode = "OFF";
            }

            List<SQLiteOpenMode> openModes = new ArrayList<>();
            if(openMode == null || openMode.length == 0) {
                openModes.add(SQLiteOpenMode.READWRITE);
                openModes.add(SQLiteOpenMode.NOMUTEX);//默认为可多个v8实例打开同一个数据库
            } else {
                for(String om : openMode) {
                    openModes.add(SQLiteOpenMode.valueOf(om));
                }
            }

            SqliteObject sql = helper.open(
                    path,
                    name,
                    SQLiteConfig.SynchronousMode.valueOf(syncMode),
                    (SQLiteOpenMode[]) openModes.toArray()
            );
            V8Object obj = new V8Object(runtime);
            obj.registerJavaMethod(queryCallback, "query");
            obj.registerJavaMethod(insertCallback, "insert");
            obj.registerJavaMethod(updateCallback, "update");
            obj.registerJavaMethod(execCallback, "exec");
            obj.registerJavaMethod(new JavaVoidCallback() {

                @Override
                public void invoke(V8Object receiver, V8Array parameters) {

                    String       name   = receiver.getString("name");
                    SqliteObject sqlObj = helper.getSqliteObject(name);
                    sqlObj.close();
                }
            }, "close");
            obj.add("name", sql.getName());
            obj.add("path", sql.getDbFile().getAbsolutePath());
            obj.add("isNew", sql.isNewDb());
            return obj;
        } catch (Exception e) {
            //e.printStackTrace();

            base.e("SQLITE 出错, 方法:%s, MSG:%s", "open", e.getMessage());
        }

        return null;
    }

    private JavaVoidCallback execCallback = new JavaVoidCallback() {

        @Override
        public void invoke(V8Object receiver, V8Array parameters) {
            final V8Object receiver2 = receiver.twin();
            final String name        = receiver.getString("name");
            final String sql         = parameters.getString(0);
            V8Function oldFun        =  (V8Function) parameters.get(1);
            final V8Function fun     = (oldFun).twin();
            oldFun.close();

            base.addSyncObj(receiver2);
            base.addSyncObj(fun);

            base.runSyncTask(new Runnable() {

                @Override
                public void run() {
                    SqliteObject sqlObj = helper.getSqliteObject(name);
                    Statement stmt = null;
                    int state = 0;
                    try {
                        stmt = sqlObj.getConn().createStatement();
                        stmt.execute(sql);

                    } catch (Exception e) {
                        //e.printStackTrace();
                        base.e("SQLITE 出错, 方法:%s, SQL:%s, MSG:%s", "exec", sql, e.getMessage());
                        state = -1;
                    } finally {
                        try {
                            if (stmt != null) {
                                stmt.close();
                            }
                        } catch (SQLException e) {
                        }
                        Message msg = new Message(EXEC_UPDATE, receiver2);
                        msg.setAttr1(fun);
                        msg.setAttr2(state);
                        base.pushMessage(msg);
                    }
                }
            });
        }
    };

    /**
     * 返回最后插入的主键id
     */
    private JavaVoidCallback insertCallback = new JavaVoidCallback() {

        @Override
        public void invoke(final V8Object receiver, V8Array parameters) {

            final V8Object receiver2 = receiver.twin();
            final String   name      = receiver.getString("name");
            V8Function oldFun        =  (V8Function) parameters.get(1);
            final V8Function fun     = (oldFun).twin();
            oldFun.close();

            base.addSyncObj(receiver2);
            base.addSyncObj(fun);

            final Object sqlParams   = parameters.get(0);//如果是数组则视为批量
            final String[] sqls;
            if(sqlParams instanceof V8Array) {
                V8Array paramsSqls = (V8Array) sqlParams;
                int total = paramsSqls.length();
                sqls = new String[total];
                for(int i = 0; i < total; i ++){
                    sqls[i] = paramsSqls.getString(i);
                }
                paramsSqls.close();

            } else {
                sqls = new String[1];
                sqls[0] = (String) sqlParams;
            }

            base.runSyncTask(new Runnable() {
                @Override
                public void run() {

                    SqliteObject sqlObj = helper.getSqliteObject(name);
                    Connection   conn   = sqlObj.getConn();
                    Statement    stmt   = null;
                    int          state  = 0;
                    try {
                        if(sqls.length > 1){
                            boolean isAuto = conn.getAutoCommit();
                            conn.setAutoCommit(false);
                            stmt = conn.createStatement();
                            int total = sqls.length;
                            for(int i = 0; i < total; i ++){
                                stmt.addBatch(sqls[i]);
                            }
                            stmt.executeBatch();
                            conn.commit();
                            conn.setAutoCommit(isAuto);
                        } else {
                            stmt = conn.createStatement();
                            state = stmt.executeUpdate(sqls[0]);
                        }
                    } catch (Exception e) {
                        try {
                            conn.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }

                        base.e("SQLITE 出错, 方法:%s, SQL:%s, MSG:%s", "insert", Arrays.toString(sqls), e.getMessage());
                        //e.printStackTrace();
                        state = -1;
                    } finally {
                        try {
                            if (stmt != null) {
                                stmt.close();
                            }
                        } catch (SQLException e) {
                        }

                        Message msg = new Message(EXEC_INSERT, receiver2);
                        msg.setAttr1(fun);
                        msg.setAttr2(state);

                        base.pushMessage(msg);
                    }
                }
            });
        }
    };

    private JavaVoidCallback updateCallback = new JavaVoidCallback() {

        @Override
        public void invoke(final V8Object receiver, V8Array parameters) {

            final V8Object receiver2 = receiver.twin();
            final String name        = receiver.getString("name");
            final String sql         = parameters.getString(0);
            V8Function oldFun        =  (V8Function) parameters.get(1);
            final V8Function fun     = (oldFun).twin();
            oldFun.close();
            base.addSyncObj(receiver2);
            base.addSyncObj(fun);

            base.runSyncTask(new Runnable() {
                @Override
                public void run() {
                    SqliteObject sqlObj = helper.getSqliteObject(name);
                    Statement stmt = null;
                    int state = 0;
                    try {
                        stmt = sqlObj.getConn().createStatement();
                        state = stmt.executeUpdate(sql);

                    } catch (Exception e) {
                        //e.printStackTrace();
                        base.e("SQLITE 出错, 方法:%s, SQL:%s, MSG:%s", "update", sql, e.getMessage());
                        state = -1;
                    } finally {
                        try {
                            if (stmt != null) {
                                stmt.close();
                            }
                        } catch (SQLException e) {
                        }
                        Message msg = new Message(EXEC_UPDATE, receiver2);
                        msg.setAttr1(fun);
                        msg.setAttr2(state);
                        base.pushMessage(msg);
                    }
                }
            });
        }
    };

    private JavaVoidCallback queryCallback = new JavaVoidCallback() {

        @Override
        public void invoke(V8Object receiver, V8Array parameters) {
            final V8Object receiver2 = receiver.twin();
            final String name        = receiver.getString("name");
            final String sql         = parameters.getString(0);
            V8Function oldFun        =  (V8Function) parameters.get(1);

            final V8Function fun     = (oldFun).twin();
            oldFun.close();

            base.addSyncObj(receiver2);
            base.addSyncObj(fun);

            base.runSyncTask(new Runnable() {
                @Override
                public void run() {
                    PreparedStatement stmt = null;
                    ResultSet rs = null;
                    SqliteObject sqlObj = helper.getSqliteObject(name);
                    ArrayList<SqliteRSObject[]> data = null;
                    int state = 0;
                    try {
                        stmt = sqlObj.getConn().prepareStatement(sql);
                        // stmt.setQueryTimeout(context.getConfig().getIntValue(C.SYSTEM,
                        // C.QUERY_TIMEOUT, 30) * 1000);
                        rs = stmt.executeQuery();
                        ResultSetMetaData md = rs.getMetaData();
                        int columnCount = md.getColumnCount(), j;
                        data = new ArrayList<SqliteRSObject[]>();
                        while (rs.next()) {
                            SqliteRSObject[] row = new SqliteRSObject[columnCount];
                            for (j = 1; j <= columnCount; j++) {
                                row[j - 1] = new SqliteRSObject(md.getColumnName(j), rs.getObject(j));
                            }
                            data.add(row);
                        }
                        state = 0;
                    } catch (Exception e) {
                        //e.printStackTrace();

                        base.e("SQLITE 出错, 方法:%s, SQL:%s, MSG:%s", "query", sql, e.getMessage());
                        state = -1;
                    } finally {
                        try {
                            if (stmt != null) {
                                stmt.close();
                            }
                            if (rs != null) {
                                rs.close();
                            }
                        } catch (SQLException e) {
                        }
                        Message msg = new Message(EXEC_QUERY, receiver2);
                        msg.setAttr1(fun);
                        msg.setAttr2(state);
                        msg.setAttr3(data);
                        base.pushMessage(msg);
                    }
                }
            });
        }
    };

}
