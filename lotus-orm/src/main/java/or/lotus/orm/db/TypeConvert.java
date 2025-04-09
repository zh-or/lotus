package or.lotus.orm.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class TypeConvert<T> {

    /**如果insert/update的 sql占位需要包裹函数时使用此方法返回, 比如 ST_PointFromWKB(?, 4326)*/
    public String sqlParam() {
        return "?";
    }

    /**
     * 从PreparedStatement中获取值
     * column或index只会传入一个
     */
    public abstract T decode(ResultSet rs, String columnName) throws SQLException;

    /**设置参数时调用*/
    public abstract void encode(PreparedStatement ps, int index, T obj) throws SQLException;
}
