package or.lotus.orm.geometry.model;

import or.lotus.orm.geometry.BaseGeo;
import or.lotus.orm.geometry.GeometryConvertToModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class PointGeo extends BaseGeo {
    public double lat;
    public double lng;

    public PointGeo() {
    }

    public PointGeo(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @Override
    public Geometry toGeometry() {
        return GeometryConvertToModel.factory.createPoint(new Coordinate(lng, lat));
    }
}
