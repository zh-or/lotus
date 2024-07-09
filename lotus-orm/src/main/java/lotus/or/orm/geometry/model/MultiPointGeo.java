package lotus.or.orm.geometry.model;

import lotus.or.orm.geometry.BaseGeo;
import lotus.or.orm.geometry.GeometryConvertToModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class MultiPointGeo extends BaseGeo {
    public PointGeo[] pointGeos;

    public MultiPointGeo() {
    }

    public MultiPointGeo(PointGeo[] pointGeos) {
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

        return GeometryConvertToModel.factory.createMultiPoint(coordinates);
    }
}
