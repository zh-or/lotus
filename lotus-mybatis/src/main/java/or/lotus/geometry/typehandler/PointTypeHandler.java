package or.lotus.geometry.typehandler;

import or.lotus.geometry.GeometryConvertToModel;
import or.lotus.geometry.model.PointGeo;
import org.apache.ibatis.type.MappedTypes;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * WKB转为Point
 */
@MappedTypes(PointGeo.class)
public class PointTypeHandler extends AbstractGeometryTypeHandler<PointGeo> {
    /**
     * 获取数据结果集时把数据库类型转换为对应的Java类型
     */
    public PointGeo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return  GeometryConvertToModel.toPointGeo(rs.getBinaryStream(columnName));
    }

    /**
     * 通过字段位置获取字段数据时把数据库类型转换为对应的Java类型
     */
    public PointGeo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return  GeometryConvertToModel.toPointGeo(rs.getBinaryStream(columnIndex));
    }

    /**
     * 调用存储过程后把数据库类型的数据转换为对应的Java类型
     */
    public PointGeo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        ByteArrayInputStream in = new ByteArrayInputStream(cs.getBytes(columnIndex));
        return  GeometryConvertToModel.toPointGeo(in);
    }

}
