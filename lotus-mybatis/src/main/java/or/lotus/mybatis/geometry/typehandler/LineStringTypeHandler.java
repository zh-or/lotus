package or.lotus.mybatis.geometry.typehandler;

import or.lotus.mybatis.geometry.GeometryConvertToModel;
import or.lotus.mybatis.geometry.model.LineStringGeo;
import org.apache.ibatis.type.MappedTypes;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * WKB转为LineString
 */
@MappedTypes(LineStringGeo.class)
public class LineStringTypeHandler extends AbstractGeometryTypeHandler<LineStringGeo> {
    @Override
    public LineStringGeo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return GeometryConvertToModel.toLineStringGeo(rs.getBinaryStream(columnName));
    }

    @Override
    public LineStringGeo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return GeometryConvertToModel.toLineStringGeo(rs.getBinaryStream(columnIndex));
    }

    @Override
    public LineStringGeo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        ByteArrayInputStream in = new ByteArrayInputStream(cs.getBytes(columnIndex));
        return GeometryConvertToModel.toLineStringGeo(in);
    }
}
