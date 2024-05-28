package or.lotus.geometry.typehandler;


import or.lotus.geometry.BaseGeo;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 字符串转为JTS对应的几何类型
 */
@MappedJdbcTypes({JdbcType.OTHER})
public abstract class AbstractGeometryTypeHandler<T extends BaseGeo> extends BaseTypeHandler<T> {

    /**
     * 把Java类型参数转换为对应的数据库类型
     *
     * @param ps        当前的PreparedStatement对象
     * @param i         当前参数位置
     * @param parameter 当前参数的Java对象
     * @param jdbcType  当前参数的数据库类型
     * @throws SQLException
     */
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        Geometry geo = parameter.toGeometry();
        geo.setSRID(4326);
        int dimension = geo.getDimension();
        dimension = (dimension == 2 || dimension == 3) ? dimension : 2;
        //有SRID的话前面会多4个字节, 但是插入数据库不需要这一节, 需要在sql中指定SRID的值
        //http://www.tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/io/WKBWriter.html
        //文档
        WKBWriter write = new WKBWriter(dimension, false);
        byte[] data = write.write(geo);
        ps.setBytes(i, data);
    }
}
