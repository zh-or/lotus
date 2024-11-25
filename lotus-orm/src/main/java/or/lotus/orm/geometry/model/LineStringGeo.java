package or.lotus.orm.geometry.model;


import com.fasterxml.jackson.annotation.JsonUnwrapped;
import or.lotus.orm.geometry.BaseGeo;
import or.lotus.orm.geometry.GeometryConvertToModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class LineStringGeo extends BaseGeo {

    @JsonUnwrapped
    public PointGeo[] pointGeos;

    public LineStringGeo() {
    }

    public LineStringGeo(PointGeo[] pointGeos) {
        this.pointGeos = pointGeos;
    }

    public PointGeo[] getPointGeos() {
        return pointGeos;
    }

    public void setPointGeos(PointGeo[] pointGeos) {
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
