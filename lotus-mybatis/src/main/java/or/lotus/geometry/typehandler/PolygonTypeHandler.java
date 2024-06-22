package or.lotus.geometry.typehandler;

import or.lotus.geometry.GeometryConvertToModel;
import or.lotus.geometry.model.PolygonGeo;
import org.apache.ibatis.type.MappedTypes;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * WKT转为Polygon
 */
@MappedTypes(PolygonGeo.class)
public class PolygonTypeHandler extends AbstractGeometryTypeHandler<PolygonGeo> {
    @Override
    public PolygonGeo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return GeometryConvertToModel.toPolygonGeo(rs.getBinaryStream(columnName));
    }

    @Override
    public PolygonGeo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return GeometryConvertToModel.toPolygonGeo(rs.getBinaryStream(columnIndex));
    }

    @Override
    public PolygonGeo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        ByteArrayInputStream in = new ByteArrayInputStream(cs.getBytes(columnIndex));
        return GeometryConvertToModel.toPolygonGeo(in);
    }
}
