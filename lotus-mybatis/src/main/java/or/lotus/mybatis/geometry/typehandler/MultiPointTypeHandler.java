package or.lotus.mybatis.geometry.typehandler;

import or.lotus.mybatis.geometry.GeometryConvertToModel;
import or.lotus.mybatis.geometry.model.MultiPointGeo;
import org.apache.ibatis.type.MappedTypes;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * WKB转为MultiPoint
 */
@MappedTypes(MultiPointGeo.class)
public class MultiPointTypeHandler extends AbstractGeometryTypeHandler<MultiPointGeo> {
    @Override
    public MultiPointGeo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return GeometryConvertToModel.toMultiPointGeo(rs.getBinaryStream(columnName));
    }

    @Override
    public MultiPointGeo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return GeometryConvertToModel.toMultiPointGeo(rs.getBinaryStream(columnIndex));
    }

    @Override
    public MultiPointGeo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        ByteArrayInputStream in = new ByteArrayInputStream(cs.getBytes(columnIndex));
        return GeometryConvertToModel.toMultiPointGeo(in);
    }
}
