package lotus.or.orm.db;

import or.lotus.common.Utils;
import or.lotus.http.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class DatabaseExecutor<T> {
    enum SqlMethod {
        SELECT, INSERT, INSERT_BATCH, UPDATE, UPDATE_BATCH, DELETE
    }

    static final Logger log = LoggerFactory.getLogger(DatabaseExecutor.class);
    SqlMethod sqlMethod;//select insert update delete
    Class<T> clazz;
    Object object;
    LotusSqlBuilder builder;
    Database db;
    ArrayList<Object> params = new ArrayList<>(20);
    ArrayList<Object> whereParams = new ArrayList<>(20);

    public DatabaseExecutor(Database db, SqlMethod m, Class<T> clazz, Object object) {
        this.db = db;
        this.sqlMethod = m;
        this.clazz = clazz;
        this.object = object;
        if(clazz == null) {
            builder = new LotusSqlBuilder(db, "unknow");
        } else {
            builder = new LotusSqlBuilder(db, JdbcUtils.convertPropertyNameToUnderscoreName(clazz.getSimpleName()));
        }
    }

    public LotusSqlBuilder getSqlBuilder() {
        return builder;
    }

    /**设置查询的字段, 只生效最后一次调用*/
    public DatabaseExecutor<T> fieldsFromObj() {
        Field[] fields = clazz.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>(fields.length);
        for(Field f : fields) {
            fieldNames.add(JdbcUtils.convertPropertyNameToUnderscoreName(f.getName()));
        }
        fieldList(fieldNames);
        return this;
    }

    /**设置查询的字段, 只生效最后一次调用*/
    public DatabaseExecutor<T> fields(String ...fs) {
        List<String> fieldNames = new ArrayList<>(fs.length);
        for(String f : fs) {
            fieldNames.add(f);
        }
        fieldList(fieldNames);
        return this;
    }

    /**设置查询的字段, 只生效最后一次调用*/
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

    /**update 和 insert 使用数据库默认值的字段 */
    public DatabaseExecutor<T> useDefaultField(String ...defs) {
        builder.addDefFields(defs);
        return this;
    }

    /**left in(right)*/
    public DatabaseExecutor<T> whereIn(Object left, Object ...right) {
        StringBuilder sb = new StringBuilder(right.length * 2);
        for(Object r : right) {
            sb.append("?,");
            whereParams.add(r);
        }
        sb.setLength(sb.length() - 1);
        builder.addWhere(WhereItem.in(left, sb.toString()));
        return this;
    }

    /**left like right*/
    public DatabaseExecutor<T> whereLike(Object left, Object right) {
       return whereLike(left, right, false);
    }

    /**left like right
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereLike(Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }

        builder.addWhere(WhereItem.like(left, "?"));
        whereParams.add(right);
        return this;
    }

    /**left is null*/
    public DatabaseExecutor<T> whereIsNULL(Object left) {
        builder.addWhere(WhereItem.isNULL(left));
        return this;
    }

    /**left is not null*/
    public DatabaseExecutor<T> whereIsNotNull(Object left) {
        builder.addWhere(WhereItem.isNotNULL(left));
        return this;
    }


    public DatabaseExecutor<T> whereNot(Object left, Object right) {
        return whereNot(left, right, false);
    }

    /**left != right
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereNot(Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }

        builder.addWhere(WhereItem.not(left, "?"));
        whereParams.add(right);
        return this;
    }
    public DatabaseExecutor<T> whereEq(Object left, Object right) {
        return whereEq(left, right, false);
    }

    /**left = right
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereEq(Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        builder.addWhere(WhereItem.eq(left, "?"));
        whereParams.add(right);
        return this;
    }


    public DatabaseExecutor<T> whereLt(Object left, Object right) {
        return whereLt(left, right, false);
    }

    /**left < right
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereLt(Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        builder.addWhere(WhereItem.lt(left, "?"));
        whereParams.add(right);
        return this;
    }


    public DatabaseExecutor<T> whereGt(Object left, Object right) {
        return whereGt(left, right, false);
    }

    /**left > right
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereGt(Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        builder.addWhere(WhereItem.gt(left, "?"));
        whereParams.add(right);
        return this;
    }

    public DatabaseExecutor<T> whereOr() {
        builder.addWhere(WhereItem.or());
        return this;
    }

    public DatabaseExecutor<T> desc(String field) {
        builder.addOrder(OrderItem.desc(field));
        return this;
    }
    public DatabaseExecutor<T> asc(String field) {
        builder.addOrder(OrderItem.asc(field));
        return this;
    }

    /**将直接输出到sql的 order by 后面*/
    public DatabaseExecutor<T> orderBy(String orderBy) {
        builder.addOrder(OrderItem.orderBy(orderBy));
        return this;
    }

    public DatabaseExecutor<T> whereAnd() {
        builder.addWhere(WhereItem.and());
        return this;
    }

    public int execute() throws SQLException {

        switch (sqlMethod) {
            case SELECT: return (int) findCount();
            case INSERT: return runInsert(builder.buildInsert());
            case INSERT_BATCH: return runInsertBatch(builder.buildInsert(), (List<Object>) object);
            case UPDATE: return runUpdate(builder.buildUpdate());
            case UPDATE_BATCH: return runUpdateBatch(builder.buildUpdate(), (List<Object>) object);
            case DELETE: return runDelete(builder.buildDelete());
        }

        return 0;
    }

    /**select count(config.primaryKeyName) from table [where xxx]*/
    public long findCount() throws SQLException {
        limit(0, 1);
        List<Map<String, Object>> res = runSelectMap(builder.buildCount());
        if(!res.isEmpty()) {
            Map<String, Object> map = res.get(0);
            return (long) map.values().toArray()[0];
        }
        return 0;
    }

    /**key from fields*/
    public Object findOneByCol(int col) throws SQLException {
        Map<String, Object> oneMap = findOneMap();
        return oneMap.get(builder.fields.get(col));
    }

    public Page<T> findPage(int page, int size) throws SQLException {
        int total = (int) findCount();
        builder.limit(Page.pageToStart(page, size), size);
        return new Page<T>(
                page,
                total,
                size,
                findList()
        );
    }

    /***/
    public DatabaseExecutor<T> limit(int start, int size) {
        builder.limit(start, size);
        return this;
    }

    /**调用此方法会自动加入 limit 0, 1*/
    public T findOne() throws SQLException {
        limit(0, 1);

        List<T> res = runSelect(builder.buildSelect(), true);
        if(res != null && res.size() > 0) {
            return res.get(0);
        }
        return null;
    }

    public List<T> findList() throws SQLException {
        return runSelect(builder.buildSelect(), false);
    }

    /**调用此方法会自动加入 limit 0, 1*/
    public Map<String, Object> findOneMap() throws SQLException {
        limit(0, 1);
        List<Map<String, Object>> list = runSelectMap(builder.buildSelect());
        if(!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public List<Map<String, Object>> findListMap() throws SQLException {
        return runSelectMap(builder.buildSelect());
    }

    private int runDelete(String sql) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("delete sql: {}, --bind({} {})", sql, params, whereParams);
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);
            JdbcUtils.setParamsToStatement(ps, whereParams, params.size());
            return ps.executeUpdate();
        }
    }


    private int runUpdate(String sql) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("update sql: {}, --bind({} {})", sql, params, whereParams);
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);
            JdbcUtils.setParamsToStatement(ps, whereParams, params.size());
            return ps.executeUpdate();
        }
    }

    private int runUpdateBatch(String sql, List<Object> list) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("update batch sql: {}, --bind({} {})", sql, params, whereParams);
        }
        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            boolean isAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            ArrayList<Object> insertParams = new ArrayList<>(20);
            String primaryKey = db.getConfig().getPrimaryKeyName();

            Object firstObject = list.get(0);
            Class<?> clazz = firstObject.getClass();
            Field[] fs = clazz.getDeclaredFields();

            for(Object obj : list) {
                insertParams.clear();
                for(Field f : fs) {
                    String fieldName = f.getName();
                    if(builder.isDefaultFields(fieldName)) {
                        continue;
                    }

                    Object val = JdbcUtils.invokeGetter(obj, clazz, fieldName);
                    insertParams.add(val);
                }
                //主键
                insertParams.add(JdbcUtils.invokeGetter(obj, clazz, primaryKey));
                JdbcUtils.setParamsToStatement(ps, insertParams);
                ps.addBatch();
            }

            int[] updateCounts = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(isAuto);
            updateCount = Arrays.stream(updateCounts).sum();

            return updateCount;
        }
    }


    private int runInsert(String sql) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("insert sql: {}, --bind({} {})", sql, params, whereParams);
        }
        ResultSet rs = null;
        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);

            updateCount = ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if(rs != null && rs.next()) {
                String primaryKey = db.getConfig().getPrimaryKeyName();
                Field field = clazz.getDeclaredField(primaryKey);
                Class<?> fieldType = field.getType();
                if(fieldType == int.class || fieldType == Integer.class) {
                    JdbcUtils.invokeSetter(object, clazz, primaryKey, (int) rs.getLong(1));
                } else if(fieldType == long.class || fieldType == Long.class) {
                    JdbcUtils.invokeSetter(object, clazz, primaryKey, rs.getLong(1));
                }
            }
            return updateCount;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
    }

    private int runInsertBatch(String sql, List<Object> list) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("insert batch sql:{}, --bind({} {})", sql, params, whereParams);
        }
        ResultSet rs = null;
        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            boolean isAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            ArrayList<Object> insertParams = new ArrayList<>(20);
            String primaryKey = db.getConfig().getPrimaryKeyName();

            Object firstObject = list.get(0);
            Class<?> clazz = firstObject.getClass();
            Field[] fs = clazz.getDeclaredFields();

            for(Object obj : list) {
                insertParams.clear();
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
                Field field = clazz.getDeclaredField(primaryKey);
                Class<?> fieldType = field.getType();
                if(fieldType == int.class || fieldType == Integer.class) {
                    JdbcUtils.invokeSetter(obj, clazz, primaryKey, (int) rs.getLong(1));
                } else if(fieldType == long.class || fieldType == Long.class) {
                    JdbcUtils.invokeSetter(obj, clazz, primaryKey, rs.getLong(1));
                }
            }
            return updateCount;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
    }

    private List<Map<String, Object>> runSelectMap(String sql) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("{}, --bind({} {})", sql, params, whereParams);
        }
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);
            JdbcUtils.setParamsToStatement(ps, whereParams, params.size());
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
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
    }

    private List<T> runSelect(String sql, boolean one) throws SQLException {
        if(db.getConfig().isPrintSqlLog()) {
            log.debug("{}, --bind({} {})", sql, params, whereParams);
        }
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            JdbcUtils.setParamsToStatement(ps, params);
            JdbcUtils.setParamsToStatement(ps, whereParams, params.size());

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
                        Field field = newClazz.getDeclaredField(fieldName);
                        //Object val = JdbcUtils.getResultSetValue(rs, i, field.getType());
                        JdbcUtils.invokeSetter(obj, newClazz, fieldName, rs.getObject(i, field.getType()));

                    } catch (NoSuchFieldException e) {
                        log.error("对象: {} 不存在字段: {}, {}", clazz.getSimpleName(), name, e);
                    }
                }
                resObj.add(obj);
                if(one) {
                    break;
                }
            }
            return resObj;
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("执行查询出错: ", e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
        return null;
    }

}
