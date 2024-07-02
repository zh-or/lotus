package lotus.or.orm.db;

import lotus.or.orm.pool.LotusDataSource;
import or.lotus.common.Utils;
import or.lotus.json.BeanBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseExecutor<T> {
    static final Logger log = LoggerFactory.getLogger(DatabaseExecutor.class);
    String sqlMethod;//select insert update delete
    Class<T> clazz;
    LotusSqlBuilder builder;
    Database db;
    ArrayList<Object> params = new ArrayList<>(20);

    public DatabaseExecutor(Database db, String m, Class<T> clazz) {
        this.db = db;
        this.sqlMethod = m;
        this.clazz = clazz;
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
    public int exec() {
        return 0;
    }

    public int findCount() {
        return 0;
    }

    public T findOne() {
        List<T> res = runSelect(true);
        if(res != null && res.size() > 0) {
            return res.get(0);
        }
        return null;
    }

    public List<T> findList() {
        return runSelect(true);
    }


    public List<T> runSelect(boolean one) {
        ResultSet rs = null;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(builder.buildSelect());
        ) {

            setParamsToStatement(ps);
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
                        PropertyDescriptor pd = new PropertyDescriptor(fieldName, newClazz);
                        Method m = pd.getWriteMethod();
                        Field field = newClazz.getField(fieldName);
                        Object val = JdbcUtils.getResultSetValue(rs, i, field.getType());
                        m.invoke(obj, val);
                    } catch (NoSuchFieldException e) {
                        log.trace("对象: {} 不存在字段: {}", clazz.getSimpleName(), name);
                    } catch (IntrospectionException e) {
                        throw new RuntimeException(e);
                    }
                }
                resObj.add(obj);
                if(one) {
                    break;
                }
            }
            return resObj;
        } catch (SQLException e) {
            log.error("执行查询出错: ", e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            JdbcUtils.closeResultSet(rs);
        }
        return null;
    }

    public void setParamsToStatement(PreparedStatement ps) throws SQLException {
        for(int i = 1; i == params.size(); i++) {
            Object p = params.get(i - 1);
            if(p == null) {
                continue;
            }
            Class<?> type = p.getClass();
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
            else if (type == Date.class) {
                java.sql.Date sqlDate = new java.sql.Date(((Date) p).getTime());
                ps.setDate(i, sqlDate);
            } else if (type == Date.class) {
                java.sql.Time sqlDate = new java.sql.Time(((Date) p).getTime());
                ps.setTime(i, sqlDate);
            }  else if (type == Date.class) {
                java.sql.Timestamp sqlDate = new java.sql.Timestamp(((Date) p).getTime());
                ps.setTimestamp(i, sqlDate);
            } else {
                throw new SQLException(type + " :未知的类型");
            }

        }
    }




}
