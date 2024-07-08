package lotus.or.orm.db;

import lotus.or.orm.pool.LotusDataSource;
import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class JdbcUtils {

    /**
     * Constant that indicates an unknown (or unspecified) SQL type.
     *
     * @see java.sql.Types
     */
    public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;

    private static final Logger log = LoggerFactory.getLogger(LotusDataSource.class);

    private static final Map<Integer, String> typeNames = new HashMap<>();

    static {
        try {
            for (Field field : Types.class.getFields()) {
                typeNames.put((Integer) field.get(null), field.getName());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to resolve JDBC Types constants", ex);
        }
    }


    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                log.debug("Could not close JDBC Statement", ex);
            } catch (Throwable ex) {
                log.debug("Unexpected exception on closing JDBC Statement", ex);
            }
        }
    }


    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                log.debug("Could not close JDBC ResultSet", ex);
            } catch (Throwable ex) {
                log.debug("Unexpected exception on closing JDBC ResultSet", ex);
            }
        }
    }

    /**执行对象的setter方法*/
    public static void invokeSetter(Object obj, Class<?> clazz, String fieldName, Object val) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(fieldName, clazz);
            Method m = pd.getWriteMethod();
            if(m != null) {
                m.invoke(obj, val);
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**执行对象的getter方法*/
    public static Object invokeGetter(Object obj, Class<?> clazz, String fieldName) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(fieldName, clazz);
            Method m = pd.getReadMethod();
            if(m != null) {
                return m.invoke(obj);
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void setParamsToStatement(PreparedStatement ps, List<Object> params) throws SQLException {
        setParamsToStatement(ps, params, 0);
    }

    public static void setParamsToStatement(PreparedStatement ps, List<Object> params, int start) throws SQLException {
        for(int j = 0; j < params.size(); j++) {
            Object p = params.get(j);

            int i = j + start + 1;

            if(p == null) {
                ps.setNull(i, Types.NULL);
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
            else if (type == java.util.Date.class) {
                ps.setTimestamp(i, new Timestamp(((java.util.Date) p).getTime()));
            } else {

                throw new SQLException(type + " :未知的类型");
            }
        }
    }

    public static Object getResultSetValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
        if (requiredType == null) {
            return getResultSetValue(rs, index);
        }

        Object value;

        // Explicitly extract typed value, as far as possible.
        if (String.class == requiredType) {
            return rs.getString(index);
        } else if (boolean.class == requiredType || Boolean.class == requiredType) {
            value = rs.getBoolean(index);
        } else if (byte.class == requiredType || Byte.class == requiredType) {
            value = rs.getByte(index);
        } else if (short.class == requiredType || Short.class == requiredType) {
            value = rs.getShort(index);
        } else if (int.class == requiredType || Integer.class == requiredType) {
            value = rs.getInt(index);
        } else if (long.class == requiredType || Long.class == requiredType) {
            value = rs.getLong(index);
        } else if (float.class == requiredType || Float.class == requiredType) {
            value = rs.getFloat(index);
        } else if (double.class == requiredType || Double.class == requiredType || Number.class == requiredType) {
            value = rs.getDouble(index);
        } else if (BigDecimal.class == requiredType) {
            return rs.getBigDecimal(index);
        } else if (java.sql.Date.class == requiredType) {
            return rs.getDate(index);
        } else if (java.sql.Time.class == requiredType) {
            return rs.getTime(index);
        } else if (java.sql.Timestamp.class == requiredType || java.util.Date.class == requiredType) {
            return rs.getTimestamp(index);
        } else if (byte[].class == requiredType) {
            return rs.getBytes(index);
        } else if (Blob.class == requiredType) {
            return rs.getBlob(index);
        } else if (Clob.class == requiredType) {
            return rs.getClob(index);
        } else if (requiredType.isEnum()) {
            // Enums can either be represented through a String or an enum index value:
            // leave enum type conversion up to the caller (e.g. a ConversionService)
            // but make sure that we return nothing other than a String or an Integer.
            /*Object obj = rs.getObject(index);
            if (obj instanceof String) {
                return obj;
            } else if (obj instanceof Number number) {
                // Defensively convert any Number to an Integer (as needed by our
                // ConversionService's IntegerToEnumConverterFactory) for use as index
                return NumberUtils.convertNumberToTargetClass(number, Integer.class);
            } else {
                // e.g. on Postgres: getObject returns a PGObject, but we need a String
                return rs.getString(index);
            }*/
            throw new SQLException("暂时不支持直接转换枚举类型");
        } else {
            // Some unknown type desired -> rely on getObject.
            try {
                return rs.getObject(index, requiredType);
            } catch (SQLFeatureNotSupportedException | AbstractMethodError ex) {
                log.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
            } catch (SQLException ex) {
                if (log.isDebugEnabled()) {
                    log.debug("JDBC driver has limited support for 'getObject(int, Class)' with column type: " + requiredType.getName(), ex);
                }
            }

            // Corresponding SQL types for JSR-310 / Joda-Time types, left up
            // to the caller to convert them (e.g. through a ConversionService).
            String typeName = requiredType.getSimpleName();
            switch (typeName) {
                case "LocalDate" : return rs.getDate(index);
                case "LocalTime" : return rs.getTime(index);
                case "LocalDateTime" : return rs.getTimestamp(index);
                // Fall back to getObject without type specification, again
                // left up to the caller to convert the value if necessary.
                default : return getResultSetValue(rs, index);
            }
        }

        // Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
        return (rs.wasNull() ? null : value);
    }

    public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
        Object obj = rs.getObject(index);
        String className = null;
        if (obj != null) {
            className = obj.getClass().getName();
        }
        if (obj instanceof Blob) {
            Blob blob = (Blob) obj;
            obj = blob.getBytes(1, (int) blob.length());
        } else if (obj instanceof Clob ) {
            Clob clob = (Clob) obj;
            obj = clob.getSubString(1, (int) clob.length());
        } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
            obj = rs.getTimestamp(index);
        } else if (className != null && className.startsWith("oracle.sql.DATE")) {
            String metaDataClassName = rs.getMetaData().getColumnClassName(index);
            if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
                obj = rs.getTimestamp(index);
            } else {
                obj = rs.getDate(index);
            }
        } else if (obj instanceof java.sql.Date) {
            if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
                obj = rs.getTimestamp(index);
            }
        }
        return obj;
    }


    /**
     * Check whether the given SQL type is numeric.
     *
     * @param sqlType the SQL type to be checked
     * @return whether the type is numeric
     */
    public static boolean isNumeric(int sqlType) {
        return (Types.BIT == sqlType || Types.BIGINT == sqlType || Types.DECIMAL == sqlType ||
                Types.DOUBLE == sqlType || Types.FLOAT == sqlType || Types.INTEGER == sqlType ||
                Types.NUMERIC == sqlType || Types.REAL == sqlType || Types.SMALLINT == sqlType ||
                Types.TINYINT == sqlType);
    }

    /**
     * Resolve the standard type name for the given SQL type, if possible.
     *
     * @param sqlType the SQL type to resolve
     * @return the corresponding constant name in {@link java.sql.Types}
     * (e.g. "VARCHAR"/"NUMERIC"), or {@code null} if not resolvable
     * @since 5.2
     */
    public static String resolveTypeName(int sqlType) {
        return typeNames.get(sqlType);
    }


    /**
     * Convert a property name using "camelCase" to a corresponding column name with underscores.
     * A name like "customerNumber" would match a "customer_number" column name.
     *
     * @param name the property name to be converted
     * @return the column name using underscores
     * @see #convertUnderscoreNameToPropertyName
     * @since 6.1
     */
    public static String convertPropertyNameToUnderscoreName(String name) {
        if (Utils.CheckNull(name)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Convert a column name with underscores to the corresponding property name using "camelCase".
     * A name like "customer_number" would match a "customerNumber" property name.
     *
     * @param name the potentially underscores-based column name to be converted
     * @return the name using "camelCase"
     * @see #convertPropertyNameToUnderscoreName
     */
    public static String convertUnderscoreNameToPropertyName(String name, boolean isFirstUpper) {
        if (Utils.CheckNull(name)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean nextIsUpper = false;
        if (name.length() > 1 && name.charAt(1) == '_') {
            result.append(Character.toUpperCase(name.charAt(0)));
        } else {
            if(isFirstUpper) {
                result.append(Character.toUpperCase(name.charAt(0)));
            } else {
                result.append(Character.toLowerCase(name.charAt(0)));
            }
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                nextIsUpper = true;
            } else {
                if (nextIsUpper) {
                    result.append(Character.toUpperCase(c));
                    nextIsUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }


    /**
     * 默认以utf-8读取文件内容
     * maven 打包需要注意在pom.xml 配置把resources的文件打包到jar中, 不配置默认不会打包, 永远找不到文件
     * @param path 不要以 / 开头,
     * */
    public static String sqlFromResources(String path) throws IOException {
        return sqlFromResources(path, "UTF-8");
    }

    /**
     * maven 打包需要注意在pom.xml 配置把resources的文件打包到jar中, 不配置默认不会打包, 永远找不到文件
     * @param path 不要以 / 开头,
     * */
    public static String sqlFromResources(String path, String charsetName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try(InputStream in = cl.getResourceAsStream(path)) {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            return new String(bytes, charsetName);
        }
    }
}
