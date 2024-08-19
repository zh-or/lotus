package or.lotus.orm.geometry.model;


import or.lotus.orm.geometry.BaseGeo;
import or.lotus.orm.geometry.GeometryConvertToModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class LineStringGeo extends BaseGeo {

    public PointGeo[] pointGeos;

    public LineStringGeo() {
    }

    public LineStringGeo(PointGeo[] pointGeos) {
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

        return GeometryConvertToModel.factory.createLineString(coordinates);
    }
}
