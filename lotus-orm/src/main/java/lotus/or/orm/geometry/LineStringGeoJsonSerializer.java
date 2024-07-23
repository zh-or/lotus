package lotus.or.orm.geometry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lotus.or.orm.geometry.model.LineStringGeo;
import lotus.or.orm.geometry.model.PointGeo;

import java.io.IOException;

public class LineStringGeoJsonSerializer extends JsonSerializer<LineStringGeo> {
    @Override
    public void serialize(LineStringGeo lineStringGeo, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartArray();
        for(PointGeo p : lineStringGeo.pointGeos) {
            jsonGenerator.writeObject(p);
        }
        jsonGenerator.writeEndArray();
    }
}



