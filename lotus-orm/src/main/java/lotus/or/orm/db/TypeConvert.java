package lotus.or.orm.db;

import java.sql.PreparedStatement;

public abstract class TypeConvert<T> {
    /**
     * 从PreparedStatement中获取值
     * @param ps
     * @param field
     * @param obj
     */
    public abstract void fromPreparedStatement(PreparedStatement ps, String field, T obj);

    /**从obj转换到PreparedStatement*/
    public abstract void toPreparedStatement(PreparedStatement ps, String field, T obj);
}
