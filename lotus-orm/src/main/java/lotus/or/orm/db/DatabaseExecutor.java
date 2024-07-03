package lotus.or.orm.db;

import or.lotus.http.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class DatabaseExecutor<T> {
    static final Logger log = LoggerFactory.getLogger(DatabaseExecutor.class);
    String sqlMethod;//select insert update delete
    Class<T> clazz;
    Object object;
    LotusSqlBuilder builder;
    Database db;
    ArrayList<Object> params = new ArrayList<>(20);

    public DatabaseExecutor(Database db, String m, Class<T> clazz, Object object) {
        this.db = db;
        this.sqlMethod = m;
        this.clazz = clazz;
        this.object = object;
        builder = new LotusSqlBuilder(JdbcUtils.convertPropertyNameToUnderscoreName(clazz.getSimpleName()));
    }

    public LotusSqlBuilder getSqlBuilder() {
        return builder;
    }

    public DatabaseExecutor<T> fieldsFromObj() {
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);
        for(Field f : fields) {
            fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(f.getName()));
        }
        fieldList(fieldNames);
        return this;
    }

    public DatabaseExecutor<T> fields(String ...fs) {
        List<String> fieldNames = new ArrayList<>(fs.length);
        for(String f : fs) {
            fieldNames.add(f);
        }
        fieldList(fieldNames);
        return this;
    }

    public DatabaseExecutor<T> fieldList(List<String> fs) {
        builder.setFields(fs);
        return this;
    }

    public DatabaseExecutor<T> params(Object ...ps) {
        for(Object p : ps) {
            params.add(p);
        }
        return this;
    }

    public DatabaseExecutor<T> useDefaultField(String ...defs) {
        builder.addDefFields(defs);
        return this;
    }

    public DatabaseExecutor<T> whereEq(String left, String right) {
        builder.addWhere(WhereItem.eq(left, right));
        return this;
    }

    public DatabaseExecutor<T> whereLt(String left, String right) {
        builder.addWhere(WhereItem.lt(left, right));
        return this;
    }

    public DatabaseExecutor<T> whereGt(String left, String right) {
        builder.addWhere(WhereItem.gt(left, right));
        return this;
    }

    public DatabaseExecutor<T> whereOr() {
        builder.addWhere(WhereItem.or());
        return this;
    }

    public DatabaseExecutor<T> whereAnd() {
        builder.addWhere(WhereItem.and());
        return this;
    }

    public int exec() {
        if("select".equals(sqlMethod)) {
            return (int) findCount();

        } else if("insert".equals(sqlMethod)) {

            String sql = builder.buildInsert();
            return runInsert(sql, (List<Object>) object);

        } else if("update".equals(sqlMethod)) {
            String sql = builder.buildUpdate();
            //return runUpdate(sql, (List<Object>) object);

        } else if("delete".equals(sqlMethod)) {
            String sql = builder.buildDelete();
            //return runUpdate(sql, (List<Object>) object);

        }
        return 0;
    }

    public long findCount() {
        List<Map<String, Object>> res = runSelectMap(builder.buildCount());
        if(!res.isEmpty()) {
            Map<String, Object> map = res.get(0);
            return (long) map.values().toArray()[0];
        }
        return 0;
    }

    public Page<T> findPage(int page, int size) {
        int total = (int) findCount();
        builder.limit(Page.pageToStart(page, size), size);
        return new Page<T>(
                page,
                total,
                size,
                findList()
        );
    }

    public T findOne() {
        List<T> res = runSelect(builder.buildSelect(), true);
        if(res != null && res.size() > 0) {
            return res.get(0);
        }
        return null;
    }

    public List<T> findList() {
        return runSelect(builder.buildSelect(), false);
    }

    public Map<String, Object> findOneMap() {
        List<Map<String, Object>> list = runSelectMap(builder.buildSelect());
        if(!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public List<Map<String, Object>> findListMap() {
        return runSelectMap(builder.buildSelect());
    }

    private int runInsert(String sql, List<Object> list) {
        log.debug("{}, --bind({})", sql, params);
        ResultSet rs = null;
        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            boolean isAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            ArrayList<Object> insertParams = new ArrayList<>(20);
            String primaryKey = db.getConfig().getPrimaryKeyName();
            for(Object obj : list) {
                insertParams.clear();
                Class<?> clazz = obj.getClass();
                Field[] fs = clazz.getDeclaredFields();
                for(Field f : fs) {
                    String fieldName = f.getName();
                    if(builder.isDefaultFields(fieldName)) {
                        continue;
                    }

                    Object val = JdbcUtils.invokeGetter(obj, clazz, fieldName);

                    insertParams.add(val);
                }
                JdbcUtils.setParamsToStatement(ps, insertParams);
                ps.addBatch();
            }

            int[] updateCounts = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(isAuto);
            updateCount = Arrays.stream(updateCounts).sum();

            rs = ps.getGeneratedKeys();
            for(int i = 0; rs.next(); i++) {
                Object obj = list.get(i);
                Class<?> clazz = obj.getClass();
                Field field = clazz.getField(primaryKey);
                Class<?> fieldType = field.getType();
                if(fieldType == int.class || fieldType == Integer.class) {
                    JdbcUtils.invokeSetter(obj, clazz, primaryKey, (int) rs.getLong(1));
                } else if(fieldType == long.class || fieldType == Long.class) {
                    JdbcUtils.invokeSetter(obj, clazz, primaryKey, rs.getLong(1));
                }
            }
            return updateCount;
        } catch (SQLException e) {
            log.error("执行查询出错: ", e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
        return updateCount;
    }

    private List<Map<String, Object>> runSelectMap(String sql) {
        log.debug("{}, --bind({})", sql, params);
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> resObj = new ArrayList<>();
            while(rs.next()) {
                Map<String, Object> obj = new HashMap<>();
                for(int i = 1; i <= columnCount; i++) {
                    String name = metaData.getColumnName(i);
                    String fieldName = JdbcUtils.convertUnderscoreNameToPropertyName(name, false);
                    obj.put(fieldName, rs.getObject(i));
                }
                resObj.add(obj);
            }
            return resObj;
        } catch (SQLException e) {
            log.error("执行查询出错: ", e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
        return null;
    }

    private List<T> runSelect(String sql, boolean one) {
        log.debug("{}, --bind({})", sql, params);
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);

            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<T> resObj = new ArrayList<T>();
            while(rs.next()) {

                T obj = clazz.newInstance();
                Class<?> newClazz = obj.getClass();
                for(int i = 1; i <= columnCount; i++) {
                    String name = metaData.getColumnName(i);
                    try {
                        String fieldName = JdbcUtils.convertUnderscoreNameToPropertyName(name, false);
                        Field field = newClazz.getField(fieldName);
                        Object val = JdbcUtils.getResultSetValue(rs, i, field.getType());
                        JdbcUtils.invokeSetter(obj, newClazz, fieldName, val);

                    } catch (NoSuchFieldException e) {
                        log.trace("对象: {} 不存在字段: {}", clazz.getSimpleName(), name);
                    }
                }
                resObj.add(obj);
                if(one) {
                    break;
                }
            }
            return resObj;
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            log.error("执行查询出错: ", e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
        return null;
    }





}
