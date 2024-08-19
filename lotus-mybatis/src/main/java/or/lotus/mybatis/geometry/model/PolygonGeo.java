package or.lotus.mybatis.geometry.model;

import or.lotus.mybatis.geometry.BaseGeo;
import or.lotus.mybatis.geometry.GeometryConvertToModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class PolygonGeo extends BaseGeo {

    public PointGeo[] pointGeos;

    public PolygonGeo() {
    }

    public PolygonGeo(PointGeo[] pointGeos) {
        this.pointGeos = pointGeos;
    }

    @Override
    public Geometry toGeometry() {
        int len = pointGeos.length;
        Coordinate[] coordinates = new Coordinate[len];
        for(int i = 0; i < len; i++) {
            PointGeo p = pointGeos[i];
            coordinates[i] = new Coordinate(p.lng, p.lat);
        }

        return GeometryConvertToModel.factory.createPolygon(coordinates);
    }
}
