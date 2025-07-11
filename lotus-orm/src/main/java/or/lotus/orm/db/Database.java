package or.lotus.orm.db;

import or.lotus.orm.pool.DataSourceConfig;
import or.lotus.orm.pool.LotusDataSource;
import or.lotus.core.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 约定:
 *  1. 数据库表名必须和类名一致
 *  2. 数据库字段名完全对应bean的字段, bean的大写会转换为 createTime => create_time
 *  3. 所有主键名必须一致, 减少无畏的判断
 *  4. bean的字段不要用基本数据类型, 全部使用包装类型 比如 int => Integer
 *
 * */
public class Database implements AutoCloseable {
    static final Logger log = LoggerFactory.getLogger(Database.class);
    ConcurrentHashMap<String, DataSource> dataSources = null;
    String name = "default";

    private DataSourceConfig config = null;

    static final ThreadLocal<Connection> transactionConnection = new ThreadLocal<Connection>();

    protected Thread queueThread;
    protected boolean isRun = true;

    public Database() {
        queueThread =  new Thread(queueRunner, "lotus-orm queue thread");
        queueThread.start();
    }

    /**如果每个DataSource的close方法不一样就多多调用几次, 数据源被关闭后将被移除注册*/
    public void close(String dataSourceCloseMethodName, Object ... args) {
        Utils.assets(dataSourceCloseMethodName, "dataSourceCloseMethodName is null");

        if(dataSources != null) {
            for(Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
                try {
                    DataSource ds = entry.getValue();
                    Method[] ms = ds.getClass().getMethods();

                    for(Method m: ms) {
                        if(dataSourceCloseMethodName.equals(m.getName())) {
                            m.invoke(ds, args);
                            dataSources.remove(entry.getKey());
                        }
                    }
                } catch (Exception e) {
                    log.error("关闭数据源 {} 出错:", entry.getKey(), e);
                }
            }
        }
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
        if(dataSources.containsKey(name)) {
            throw new RuntimeException("当前数据源已存在 name:" + name);
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
            throw new SQLException("当前数据源未找到, 请检查是否注册:" + name);
        }
        return ds;
    }

    public Connection getConnection() throws SQLException {
        DataSource dataSource = getDataSource();
        //如果开启了事物则从线程本地对象取被代理过的连接对象
        Connection connection = transactionConnection.get();
        if(connection == null) {
            connection = dataSource.getConnection();
        }

        return connection;
    }

    /** 当前是否在事务范围内 */
    public boolean isInTransaction() {
        Connection connection = transactionConnection.get();
        return connection != null;
    }

    /**事物一定要调用close
     * 通常用法
     * try(Transaction transaction = db.beginTransaction()) {
     *     //todo
     *     transaction.commit();
     * }
     *
     * */
    public Transaction beginTransaction() throws SQLException {
        Transaction transaction = new Transaction(this);
        return transaction;
    }

    public DatabaseExecutor execSqlUpdate(String sql) throws SQLException {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.UPDATE, null, null);
        exec.getSqlBuilder().setSql(sql);
        return exec;
    }

    /**执行自定义查询sql时 只能使用map返回*/
    public DatabaseExecutor execSqlSelect(String sql) {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.SELECT, null, null);
        exec.getSqlBuilder().setSql(sql);
        return exec;
    }

    public <T> DatabaseExecutor<T> selectDto(Class<T> clazz, String sql)  {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.SELECT, clazz, null);
        exec.getSqlBuilder().setSql(sql);
        exec.fieldsFromObj();
        return exec;
    }

    public <T> T select(Class<T> clazz, Object id)  throws SQLException {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.SELECT, clazz, null);
        exec.fieldsFromObj();
        exec.whereEq("id", id);
        return (T) exec.findOne();
    }

    public <T> DatabaseExecutor<T> select(Class<T> clazz)  {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.SELECT, clazz, null);

        exec.fieldsFromObj();
        return exec;
    }
    public void deleteOrError(Class<?> clazz, Object id) throws SQLException {
        if(delete(clazz, id) < 1) {
            throw new SQLException("受影响行数小于1");
        }
    }

    /**根据主键删除*/
    public int delete(Class<?> clazz, Object id) throws SQLException {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.DELETE, clazz, null);
        exec.whereEq(getConfig().getPrimaryKeyName(), id);
        return exec.execute();
    }

    /**如果直接执行会删除所有数据!!!
     * 实际使用一般会 delete(Test.class).whereEq("id", 1).whereLt("xx", 1).execute()*/
    public DatabaseExecutor delete(Class<?> clazz) throws SQLException {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.DELETE, clazz, null);

        return exec;
    }

    /**使用config.primaryKeyName 作为主键更新所有字段*/
    public int updateAll(List<? extends Object> obj) throws SQLException {
        if(obj == null || obj.size() == 0) {
            return 0;
        }
        Object first = obj.get(0);
        Class<?> clazz = first.getClass();
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.UPDATE_BATCH, clazz, obj);
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);
        String primaryKeyName = getConfig().getPrimaryKeyName();
        String fieldName;
        for(Field f : fields) {
            int modifiers = f.getModifiers();
            if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            fieldName = f.getName();
            if(!fieldName.equals(primaryKeyName)) {
                fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(fieldName));
            }
        }
        exec.fieldList(fieldNames);
        exec.useDefaultField(primaryKeyName);
        exec.whereEq(primaryKeyName, "?");
        return exec.execute();
    }

    /** update(Test.class, "str", "updateTime").params("str1", new Date()).execute() */
    public DatabaseExecutor update(Object obj, String ...fields) throws SQLException {
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.UPDATE, obj.getClass(), obj);
        exec.fieldList(Arrays.asList(fields));

        String primaryKeyName = getConfig().getPrimaryKeyName();
        Class clazz = obj.getClass();
        Object val = JdbcUtils.invokeGetter(obj, clazz, primaryKeyName);
        if(val != null) {
            exec.whereEq(primaryKeyName, val);
        }

        return exec;
    }

    /**受影响行数小于1则抛出异常*/
    public void updateOrError(Object obj) throws SQLException {
        if(update(obj) < 1) {
            throw new SQLException("受影响行数小于1");
        }
    }

    /**根据对象的主键更新对象不为null的值, 如果没有主键会更新整个表的数据*/
    public int update(Object obj) throws SQLException {
        return update(obj, getConfig().isUpdateIgnoreNull());
    }

    /**受影响行数小于1则抛出异常*/
    public void updateOrError(Object obj, boolean updateIgnoreNull) throws SQLException {
        if(update(obj, updateIgnoreNull) < 1) {
            throw new SQLException("受影响行数小于1");
        }
    }

    /**
     * 根据对象的主键更新, 如果没有主键会更新整个表的数据
     * @param updateIgnoreNull 更新时如果有字段为null则忽略该字段
     */
    public int update(Object obj, boolean updateIgnoreNull) throws SQLException {
        return updateByExec(obj, updateIgnoreNull).execute();
    }

    /**根据对象的主键更新, 如果没有主键会更新整个表的数据
     * 可增加where条件, 但是必须先 whereAnd() | whereOr() 因为第一个条件是 id = ?
     * */
    public DatabaseExecutor updateByExec(Object obj) {
        return updateByExec(obj, getConfig().isUpdateIgnoreNull());
    }


    /**根据对象的主键更新, 如果没有主键会更新整个表的数据 不会更新final,static修饰的字段
     * 可增加where条件, 但是必须先 whereAnd() | whereOr() 因为第一个条件是 id = ?
     * @param updateIgnoreNull 更新时如果有字段为null则忽略该字段
     * */
    public DatabaseExecutor updateByExec(Object obj, boolean updateIgnoreNull) {
        Class<?> clazz = obj.getClass();
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.UPDATE, clazz, obj);
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);

        String primaryKeyName = getConfig().getPrimaryKeyName();
        String fieldName;
        for(Field f : fields) {
            int modifiers = f.getModifiers();
            if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            fieldName = f.getName();
            Object val = JdbcUtils.invokeGetter(obj, clazz, fieldName);
            if(fieldName.equals(primaryKeyName)) {
                if(val != null) {
                    exec.whereEq(primaryKeyName, val);
                }
            } else {
                if(val == null && updateIgnoreNull) {
                    //更新时如果有字段为null则忽略该字段
                    continue;
                }
                fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(fieldName));
                exec.params(val);
            }
        }
        exec.fieldList(fieldNames);
        exec.useDefaultField(primaryKeyName);
        return exec;
    }

    public void insertAllOrError(List<? extends  Object> obj, String ...useDefaultField) throws SQLException {
        int t = insertAll(obj, useDefaultField);
        int t1 = obj.size();

        if(t != t1) {
            throw new SQLException("批量插入有失败, 目标数量: " + t1 + " 实际插入:" + t);
        }
    }

    /**useDefaultField的字段为数据库默认值不从obj取
     * @param useDefaultField 字段为数据库字段名
     * */
    public int insertAll(List<? extends  Object> obj, String ...useDefaultField) throws SQLException {
        if(obj == null || obj.size() == 0) {
            return 0;
        }
        Object first = obj.get(0);
        Class<?> clazz = first.getClass();
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.INSERT_BATCH, clazz, obj);

        String primaryKeyName = getConfig().getPrimaryKeyName();
        exec.useDefaultField(primaryKeyName);
        exec.useDefaultField(useDefaultField);

        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);
        for(Field f : fields) {
            int modifiers = f.getModifiers();
            if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(f.getName()));
        }
        exec.fieldList(fieldNames);
        exec.useDefaultField(getConfig().getPrimaryKeyName());
        return exec.execute();
    }

    /**插入时受影响行数小于1则抛出异常*/
    public void insertOrError(Object obj, String ...useDefaultField) throws SQLException {
        if(insert(obj, useDefaultField) < 1) {
            throw new SQLException("插入失败");
        }
    }

    /**useDefaultField的字段为数据库默认值不从obj取
     * @param useDefaultField 字段为数据库字段名
     * */
    public int insert(Object obj, String ...useDefaultField) throws SQLException {
        Class<?> clazz = obj.getClass();
        DatabaseExecutor exec = new DatabaseExecutor(this, DatabaseExecutor.SqlMethod.INSERT, clazz, obj);
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);

        String primaryKeyName = getConfig().getPrimaryKeyName();
        exec.useDefaultField(primaryKeyName);
        exec.useDefaultField(useDefaultField);

        for(Field f : fields) {
            int modifiers = f.getModifiers();
            if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            String name = f.getName();
            Object val = JdbcUtils.invokeGetter(obj, clazz, name);
            if(val != null) {
                fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(name));
                if(!exec.getSqlBuilder().isDefaultFields(name)) {
                    exec.params(val);
                }
            }
        }
        exec.fieldList(fieldNames);

        return exec.execute();
    }

    LinkedBlockingQueue<InertQueueObj> queue = new LinkedBlockingQueue();

    /**
     * 插入到一个队列, 不会在当前线程执行插入
     * @return 返回当前队列大小*/
    public int insertToQueue(Object obj) {
        queue.add(new InertQueueObj(obj));
        return queue.size();
    }

    /**
     * 插入到一个队列, 不会在当前线程执行插入
     * @return 返回当前队列大小*/
    public int insertToQueue(InertQueueObj obj) {
        queue.add(obj);
        return queue.size();
    }

    private Runnable queueRunner = new Runnable() {
        @Override
        public void run() {
            //todo 需要修改为批量插入
            List<InertQueueObj> tmpList = new ArrayList<>(100);
            while(isRun || queue.size() > 0) {
                try {
                    do {
                        InertQueueObj obj = queue.poll(1, TimeUnit.SECONDS);
                        if(obj != null) {
                            tmpList.add(obj);
                        } else {
                            break;
                        }
                    } while(true);
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    log.error("从队列保存到数据库出错:", e);
                }

                if(tmpList.size() > 0) {
                    InertQueueObj lastObj = null;
                    try(Transaction transaction = beginTransaction()) {
                        for(InertQueueObj obj : tmpList) {
                            lastObj = obj;
                            int r = insert(obj.obj);
                            if(obj.callback != null) {
                                if(r > 0) {
                                    obj.callback.success(obj.obj);
                                } else {
                                    obj.callback.fail(new Exception("插入操作返回的 0"));
                                }
                            }
                        }
                        transaction.commit();
                    } catch (SQLException e) {
                        if(lastObj != null && lastObj.callback != null) {
                            lastObj.callback.fail(e);
                        } else {
                            log.error("队列批量插入失败!");
                        }
                    }
                    tmpList.clear();
                }
            }
        }
    };

    @Override
    public void close() throws Exception {
        isRun = false;
        queueThread.interrupt();
        queueThread.join();
    }
}
