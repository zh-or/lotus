package or.lotus.orm.db;

import or.lotus.core.common.Utils;
import or.lotus.orm.pool.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.math.BigDecimal;
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
    DataSourceConfig config;
    ArrayList<Object> params = new ArrayList<>(20);
    ArrayList<Object> whereParams = new ArrayList<>(20);

    public DatabaseExecutor(Database db, SqlMethod m, Class<T> clazz, Object object) {
        this.db = db;
        this.config = db.getConfig();
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
            int modifiers = f.getModifiers();
            if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
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

    /** 指定索引 */
    public DatabaseExecutor<T> useIndex(String... indexs) {
        builder.useIndex(indexs);
        return this;
    }


    public DatabaseExecutor<T> params(Object ...ps) {
        for(Object p : ps) {
            Class<?> paramType = p.getClass();
            String paramTypeName = paramType.getName();
            TypeConvert convert = config.getTypeConvert(paramTypeName);
            if(convert != null) {
                String sqlHolder = convert.sqlParam();
                if(!Utils.CheckNull(sqlHolder)) {
                    builder.addHolder(params.size(), sqlHolder);
                }
            }
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

    /**left like right*/
    public DatabaseExecutor<T> whereLike(Object left, Object right, boolean addPs) {
        return whereLike(left, right, addPs, false);
    }

    /**left like right
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereLike(Object left, Object right, boolean addPs, boolean ifRightNotEmpty) {
        return whereLike(null, left, right, addPs, ifRightNotEmpty);
    }

    /**left like right
     * @param before 如果 right 不为空, 并且 before不为空 则添加在 like前面, 用于多条件时使用
     * @param  addPs right两边添加%
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereLike(WhereItem before, Object left, Object right, boolean addPs, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        if(before != null && builder.whereSize() > 0) {
            builder.addWhere(before);
        }
        builder.addWhere(WhereItem.like(left, "?"));
        if(addPs) {
            whereParams.add("%" + right + "%");
        } else {
            whereParams.add(right);
        }
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
        return whereNot(left, right, ifRightNotEmpty);
    }

    /**left != right
     * @param before 如果 right 不为空, 并且 before不为空 则添加在 like前面, 用于多条件时使用
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereNot(WhereItem before, Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        if(before != null && builder.whereSize() > 0) {
            builder.addWhere(before);
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
        return whereEq(null, left, right, ifRightNotEmpty);
    }

    /**left = right
     * @param before 如果 right 不为空, 并且 before不为空 则添加在 like前面, 用于多条件时使用
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereEq(WhereItem before, Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        if(before != null && builder.whereSize() > 0) {
            builder.addWhere(before);
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
        return whereLt(null, left, right, ifRightNotEmpty);
    }

    /**left < right
     * @param before 如果 right 不为空, 并且 before不为空 则添加在 like前面, 用于多条件时使用
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereLt(WhereItem before, Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        if(before != null && builder.whereSize() > 0) {
            builder.addWhere(before);
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
        return whereGt(null, left, right, ifRightNotEmpty);
    }

    /**left > right
     * @param before 如果 right 不为空, 并且 before不为空 则添加在 like前面, 用于多条件时使用
     * @param ifRightNotEmpty 如果为true则增加空判断, 如果为空则不加入条件
     * */
    public DatabaseExecutor<T> whereGt(WhereItem before, Object left, Object right, boolean ifRightNotEmpty) {
        if(ifRightNotEmpty) {
            if(right == null) {
                return this;
            }

            if(right instanceof String && Utils.CheckNull((String) right)) {
                return this;
            }
        }
        if(before != null && builder.whereSize() > 0) {
            builder.addWhere(before);
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

    public DatabaseExecutor<T> where(WhereItem where) throws SQLException {
        if(where.v == null) {
            throw new SQLException("v 不能为空");
        } else {
            whereParams.add(where.v);
            where.v = "?";
            builder.addWhere(where);
        }

        return this;
    }

    /**如果受影响行数小于 1 则抛出异常*/
    public void executeOrError() throws SQLException {
        executeOrError(1);
    }

    /**如果受影响行数小于 @Param minRows 则抛出异常*/
    public void executeOrError(int minRows) throws SQLException {
        if(execute() < minRows) {
            throw new SQLException("sql执行受影响行数小于:" + minRows);
        }
    }

    /**取结果集的第一行第一列*/
    public Object execAndGet() throws SQLException {
        return execAndGet(1, 1);
    }

    /**从1开始计数*/
    public Object execAndGet(int row, int col) throws SQLException {
        List<Map<String, Object>> list = findListMap();
        if(list != null && list.size() >= row) {
            Map<String, Object> item = list.get(row - 1);
            Iterator<Map.Entry<String, Object>> it = item.entrySet().iterator();
            int i = 1;
            while(it.hasNext()) {
                if(i == col) {
                    return it.next().getValue();
                }
                i++;
            }
        }
        return null;
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

    public DatabaseExecutor table(String table) {
        builder.table = table;
        return this;
    }

    /**select count(config.primaryKeyName) from table [where xxx]
     * 此方法可以在findList/findMap/findListMap前调用, table 为 select方法传入的对象
     * */
    public long findCount() throws SQLException {
        return findCount(config.getPrimaryKeyName(), builder.table);
    }


    /**select count(countField) from table [where xxx]
     * 此方法可以在findList/findMap/findListMap前调用
     * 如果是联表查询不要使用此方法, 有局限性
     * */
    public long findCount(String countField, String table) throws SQLException {
        //limit(0, 1);
        List<Map<String, Object>> res = runSelectMap(builder.buildCount(countField, table));
        if(!res.isEmpty()) {
            Map<String, Object> map = res.get(0);
            return (long) map.values().toArray()[0];
        }
        return 0;
    }

    /**key col从0开始*/
    public Object findOneByCol(int col) throws SQLException {
        Map<String, Object> oneMap = findOneMap();
        return oneMap.values().toArray()[0];
    }

    /**key */
    public Object findOneByCol(String column) throws SQLException {
        Map<String, Object> oneMap = findOneMap();
        return oneMap.get(column);
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

    /**调用此方法会自动加入 limit 0, 1, 返回的是 LinkedHashMap 此map是有序的*/
    public Map<String, Object> findOneMap() throws SQLException {
        limit(0, 1);
        List<Map<String, Object>> list = runSelectMap(builder.buildSelect());
        if(!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**返回的是 LinkedHashMap 此map是有序的*/
    public List<Map<String, Object>> findListMap() throws SQLException {
        return runSelectMap(builder.buildSelect());
    }

    private int runDelete(String sql) throws SQLException {
        printSqlLog(sql);
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            setParamsToStatement(ps, params);
            setParamsToStatement(ps, whereParams, params.size());
            return ps.executeUpdate();
        }
    }

    private int runUpdate(String sql) throws SQLException {
        printSqlLog(sql);

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            setParamsToStatement(ps, params);
            setParamsToStatement(ps, whereParams, params.size());
            return ps.executeUpdate();
        }
    }

    private int runUpdateBatch(String sql, List<Object> list) throws SQLException {
        printSqlLog(sql);

        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            boolean isAuto = conn.getAutoCommit();
            if(!db.isInTransaction()) {
                conn.setAutoCommit(false);
            }

            ArrayList<Object> insertParams = new ArrayList<>(20);
            String primaryKey = config.getPrimaryKeyName();

            Object firstObject = list.get(0);
            Class<?> clazz = firstObject.getClass();
            Field[] fs = clazz.getDeclaredFields();

            for(Object obj : list) {
                insertParams.clear();
                for(Field f : fs) {
                    int modifiers = f.getModifiers();
                    if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    String fieldName = f.getName();
                    if(builder.isDefaultFields(JdbcUtils.convertPropertyNameToUnderscoreName(fieldName))) {
                        continue;
                    }

                    Object val = JdbcUtils.invokeGetter(obj, clazz, fieldName);
                    insertParams.add(val);
                }
                //主键
                insertParams.add(JdbcUtils.invokeGetter(obj, clazz, primaryKey));
                setParamsToStatement(ps, insertParams);
                ps.addBatch();
            }

            int[] updateCounts = ps.executeBatch();
            if(!db.isInTransaction()) {
                conn.commit();
                conn.setAutoCommit(isAuto);
            }
            updateCount = Arrays.stream(updateCounts).sum();

            return updateCount;
        }
    }


    private int runInsert(String sql) throws SQLException {
        printSqlLog(sql);
        ResultSet rs = null;
        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {

            setParamsToStatement(ps, params);

            updateCount = ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if(rs != null && rs.next()) {
                String primaryKey = config.getPrimaryKeyName();
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
        printSqlLog(sql);
        ResultSet rs = null;
        int updateCount = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            boolean isAuto = conn.getAutoCommit();
            if(!db.isInTransaction()) {
                conn.setAutoCommit(false);
            }

            ArrayList<Object> insertParams = new ArrayList<>(20);
            String primaryKey = config.getPrimaryKeyName();

            Object firstObject = list.get(0);
            Class<?> clazz = firstObject.getClass();
            Field[] fs = clazz.getDeclaredFields();

            for(Object obj : list) {
                insertParams.clear();
                for(Field f : fs) {
                    int modifiers = f.getModifiers();
                    if(Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    String fieldName = f.getName();
                    if(builder.isDefaultFields(JdbcUtils.convertPropertyNameToUnderscoreName(fieldName))) {
                        continue;
                    }

                    Object val = JdbcUtils.invokeGetter(obj, clazz, fieldName);

                    insertParams.add(val);
                }
                setParamsToStatement(ps, insertParams);
                ps.addBatch();
            }

            int[] updateCounts = ps.executeBatch();
            if(!db.isInTransaction()) {
                conn.commit();
                conn.setAutoCommit(isAuto);
            }
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
        printSqlLog(sql);
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            setParamsToStatement(ps, params);
            setParamsToStatement(ps, whereParams, params.size());
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            int mapSize = (int) (columnCount / 0.75 + 1);
            List<Map<String, Object>> resObj = new ArrayList<>();
            while(rs.next()) {
                Map<String, Object> obj = new LinkedHashMap<>(mapSize);
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
        printSqlLog(sql);
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {

            setParamsToStatement(ps, params);
            setParamsToStatement(ps, whereParams, params.size());

            rs = ps.executeQuery();

            Map<Object, Object> rowPrimaryCache = new HashMap<>();

            List<T> resObj = new ArrayList<T>();

            while(rs.next()) {

                Object res = resultSetToBean(rs, clazz, rowPrimaryCache, null, config.getPrimaryKeyName(), 0);
                if(res != null) {
                    resObj.add((T) res);
                }
                if(one) {
                    break;
                }
            }
            return resObj;
        } catch (Exception e) {
            log.error("执行查询出错: ", e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
        return null;
    }


    private Object resultSetToBean(ResultSet rs, Class<?> clazz,  Map<Object, Object> cache, String currentPrefix, String currentPrimaryKey, int lvl) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        ToMany toMany = clazz.getAnnotation(ToMany.class);
        boolean isFromCache = true;//当从缓存取到对象后, 就不返回了, 实现一对多
        if(currentPrefix == null) {
            currentPrefix = "";
        }
        Object bean = null;
        if(toMany != null) {
            String mcKey = toMany.primaryKey();
            if(!Utils.CheckNull(mcKey)) {
                currentPrimaryKey = mcKey;
            }
        }
        // step1 从缓存取对象
        Object primaryColumnValue = null;
        try {
            primaryColumnValue = rs.getObject(currentPrimaryKey, String.class);
            if(primaryColumnValue == null && lvl > 0) {
                //主键为空则认为未查询到数据
                return null;
            }
            bean = cache.get(primaryColumnValue);
        } catch (SQLException e) {
            /*如果rs结果集没有 主键 则会报错*/
        }

        // step2 未取到表示新的数据
        if(bean == null) {
            Constructor constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            bean = constructor.newInstance();
            if(primaryColumnValue != null) {
                cache.put(primaryColumnValue, bean);
            }
            isFromCache = false;
        }

        //step3 获取当前对象有哪些是需要展开处理的
        HashMap<String, String> mcColumnAndPrefix = new HashMap<>();
        HashMap<String, String> mcPrimaryKeys = new HashMap<>();
        if(toMany != null) {
            String[] mcColumns = toMany.columns();
            String[] mcPrefix = toMany.prefix();
            String[] mcKeys = toMany.primaryKeys();
            for(int i = 0; i < mcColumns.length; i++) {
                String prefix = null;
                if(i < mcPrefix.length) {
                    prefix = mcPrefix[i];
                }
                mcColumnAndPrefix.put(mcColumns[i], prefix);
                mcPrimaryKeys.put(mcColumns[i], mcKeys[i]);
            }
            //isManyColumn = mcColumns.length > 0;//如果没有字段就不展开
        }

        Field[] fields = clazz.getDeclaredFields();
        for(Field f : fields) {
            String rawFieldName = f.getName();
            String rsFieldName = JdbcUtils.convertPropertyNameToUnderscoreName(rawFieldName);
            String prefix = mcColumnAndPrefix.get(rsFieldName);
            if(prefix != null) {
                rsFieldName = prefix + rsFieldName;
            }

            //step4 判断是否需要处理的字段
            if(mcColumnAndPrefix.containsKey(rawFieldName)) {

                Type genericType = f.getGenericType();
                if(JdbcUtils.isRightList(genericType)) {//列表类型?
                    List<Object> listField = (List<Object>) JdbcUtils.invokeGetter(bean, clazz, rawFieldName);
                    if(listField == null) {
                        listField = new ArrayList<>();
                        JdbcUtils.invokeSetter(bean, clazz, rawFieldName, listField);
                    }

                    //取泛型类型
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                    Class fieldGenericType = (Class<?>) actualTypeArgs[0];

                    //下级缓存
                    HashMap<Object, Object> childCache = (HashMap<Object, Object>) cache.get(rawFieldName);
                    if(childCache == null) {
                        childCache = new HashMap<>();
                        childCache.put(rawFieldName, childCache);
                    }
                    Object listItem = resultSetToBean(rs, fieldGenericType, childCache, mcColumnAndPrefix.get(rawFieldName), mcPrimaryKeys.get(rawFieldName), lvl + 1);
                    if(listItem != null) {
                        listField.add(listItem);
                    }

                } else {//单独对象?
                    Object signItem = resultSetToBean(rs, f.getType(), new HashMap<>(), mcColumnAndPrefix.get(rawFieldName), mcPrimaryKeys.get(rawFieldName), lvl + 1);
                    JdbcUtils.invokeSetter(bean, clazz, rawFieldName, signItem);
                }

            } else {
                try {
                    Class<?> fieldTypeClass = f.getType();

                    TypeConvert convert = config.getTypeConvert(fieldTypeClass.getName());
                    if(convert != null) {
                        JdbcUtils.invokeSetter(bean, clazz, rawFieldName, convert.decode(rs, currentPrefix + rsFieldName));
                    } else {
                        Object val = rs.getObject(currentPrefix + rsFieldName, fieldTypeClass);
                        JdbcUtils.invokeSetter(bean, clazz, rawFieldName, val);
                    }
                } catch (SQLException e) {
                    /*sql未取到值*/
                }
            }
        }
        if(isFromCache) {
            return null;
        }
        return bean;
    }

    private void printSqlLog(String sql) {
        if(config.isPrintSqlLog()) {
            StringBuilder sb = new StringBuilder(2048);
            sb.append("\n┌================================== Lotus ORM ===========================================");
            if(config.isPrintStack()) {
                StackTraceElement stack[] = Thread.currentThread().getStackTrace();
                String printStackPackagePrefix = config.getPrintStackPackagePrefix();
                boolean foundPrefix = false;
                for(int i = 1; i < stack.length; i++) {
                    if(printStackPackagePrefix != null) {
                        if(stack[i].getClassName().startsWith(printStackPackagePrefix)) {
                            foundPrefix = true;
                            sb.append("\n\t");
                            sb.append(stack[i].toString());
                        } else if(foundPrefix) {
                            //如果遇到了其他包则跳出循环, 以免误导(堆栈会和实际链路不一样)
                            break;
                        }
                    } else if(!stack[i].getClassName().startsWith("lotus.or.orm.db")) {
                        sb.append("\n\t");
                        sb.append(stack[i].toString());
                    }
                }
            }

            sb.append("\n\tSQL ===> ");
            sb.append(sql);
            sb.append("\n\tbind(");
            sb.append(params);
            sb.append(" ");
            sb.append(whereParams);
            sb.append(" )");

            sb.append("\n└========================================================================================\n");
            log.debug(sb.toString());
        }

    }

    private void setParamsToStatement(PreparedStatement ps, List<Object> params) throws SQLException {
        setParamsToStatement(ps, params, 0);
    }

    private void setParamsToStatement(PreparedStatement ps, List<Object> params, int start) throws SQLException {

        for(int j = 0; j < params.size(); j++) {
            Object p = params.get(j);

            int i = j + start + 1;

            if(p == null) {
                ps.setNull(i, Types.NULL);
                continue;
            }

            Class<?> type = p.getClass();
            //转换
            if(config != null) {
                String classFullName = type.getName();
                TypeConvert convert = config.getTypeConvert(classFullName);
                if(convert != null) {
                    convert.encode(ps, i, p);
                    continue ;
                }
            }

            if (type == String.class)
                ps.setString(i, (String) p);
            else if (type == BigDecimal.class)
                ps.setBigDecimal(i, (BigDecimal) p);
            else if (type == boolean.class || type == Boolean.class)
                ps.setBoolean(i, (Boolean) p);
            else if (type == long.class || type == Long.class)
                ps.setLong(i, (Long) p);
            else if (type == int.class || type == Integer.class)
                ps.setInt(i, (Integer) p);
            else if (type == char.class)
                ps.setString(i, String.valueOf((char) p));
            else if (type == byte.class || type == Byte.class)
                ps.setByte(i, (Byte) p);
            else if (type == byte[].class || type == Byte[].class)
                ps.setBytes(i, (byte[]) p);
            else if (type == short.class || type == Short.class)
                ps.setShort(i, (Short) p);
            else if (type == float.class || type == Float.class)
                ps.setFloat(i, (Float) p);
            else if (type == double.class || type == Double.class)
                ps.setDouble(i, (Double) p);
            else if (type == java.util.Date.class) {
                ps.setTimestamp(i, new Timestamp(((java.util.Date) p).getTime()));
            } else {

                throw new SQLException(type + " :未知的类型");
            }
        }
    }
}
