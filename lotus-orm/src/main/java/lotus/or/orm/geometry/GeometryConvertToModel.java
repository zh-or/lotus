package lotus.or.orm.geometry;

import lotus.or.orm.geometry.model.LineStringGeo;
import lotus.or.orm.geometry.model.MultiPointGeo;
import lotus.or.orm.geometry.model.PointGeo;
import lotus.or.orm.geometry.model.PolygonGeo;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.InputStreamInStream;
import org.locationtech.jts.io.WKBReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class GeometryConvertToModel {
    public static GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
    static final Logger log = LoggerFactory.getLogger(GeometryConvertToModel.class);

    public static PointGeo toPointGeo(InputStream geoStream) {

        if(geoStream == null) {
            return null;
        }
        Point rawPoint = getGeometryFromWkb(Point.class, geoStream);

        return new PointGeo(
                rawPoint.getY(),
                rawPoint.getX()
        );
    }

    public static LineStringGeo toLineStringGeo(InputStream geoStream) {

        if(geoStream == null) {
            return null;
        }

        LineString rawLineString = getGeometryFromWkb(LineString.class, geoStream);
        Coordinate[] coordinates = rawLineString.getCoordinates();
        int len = coordinates.length;
        PointGeo[] pointGeos = new PointGeo[len];
        for(int i = 0; i < len; i++) {
            pointGeos[i] = new PointGeo(
                    coordinates[i].getY(),
                    coordinates[i].getX()
            );
        }

        return new LineStringGeo(pointGeos);
    }

    public static MultiPointGeo toMultiPointGeo(InputStream geoStream) {

        if(geoStream == null) {
            return null;
        }

        MultiPoint rawMultiPoint = getGeometryFromWkb(MultiPoint.class, geoStream);
        Coordinate[] coordinates = rawMultiPoint.getCoordinates();
        int len = coordinates.length;
        PointGeo[] pointGeos = new PointGeo[len];
        for(int i = 0; i < len; i++) {
            pointGeos[i] = new PointGeo(
                    coordinates[i].getY(),
                    coordinates[i].getX()
            );
        }

        return new MultiPointGeo(pointGeos);
    }


    public static PolygonGeo toPolygonGeo(InputStream geoStream) {

        if(geoStream == null) {
            return null;
        }

        Polygon rawPolygon = getGeometryFromWkb(Polygon.class, geoStream);
        Coordinate[] coordinates = rawPolygon.getCoordinates();
        int len = coordinates.length;
        PointGeo[] pointGeos = new PointGeo[len];
        for(int i = 0; i < len; i++) {
            pointGeos[i] = new PointGeo(
                    coordinates[i].getY(),
                    coordinates[i].getX()
            );
        }

        return new PolygonGeo(pointGeos);
    }

    // 从WKB中获取原始空间对象
    private static <E extends Geometry> E getGeometryFromWkb(Class<E> obj, InputStream stream) {
        try {

            if(stream == null) {
                return null;
            }
            //https://www.tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/io/WKBReader.html
            //文档
            WKBReader reader = new WKBReader(factory);

            InputStreamInStream wkb = new InputStreamInStream(stream);
            //srid

            //http://www.dev-garden.org/2011/11/27/loading-mysql-spatial-data-with-jdbc-and-jts-wkbreader/
            byte[] sridBytes = new byte[4];
            wkb.read(sridBytes);
            int srid = 0, srid2 = 0;
            for (int i = 0; i < sridBytes.length; i++) {
                srid = (srid << 8) + (sridBytes[i] & 0xff);
            }

            for (int i = 0; i < sridBytes.length; i++) {
                srid2 += (sridBytes[i] & 0xff) << (8 * i);
            }
            //System.out.println("srid1:" + srid + "  srid2:" + srid2);
            E res = (E) reader.read(wkb);
            res.setSRID(srid2);
            return res;
        } catch (Exception e) {
            log.error("从流转换到空间类型出错: ", e);
        }
        return null;
    }
}
