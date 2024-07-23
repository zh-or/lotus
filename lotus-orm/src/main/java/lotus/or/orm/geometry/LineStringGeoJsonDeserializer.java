package lotus.or.orm.geometry;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lotus.or.orm.geometry.model.LineStringGeo;
import lotus.or.orm.geometry.model.PointGeo;

import java.io.IOException;
import java.util.ArrayList;

public class LineStringGeoJsonDeserializer extends JsonDeserializer<LineStringGeo> {

    @Override
    public LineStringGeo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        ArrayNode an = oc.readTree(jsonParser);
        int size = an.size();
        ArrayList<PointGeo> ps = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            JsonNode item = an.get(i);
            ps.add(new PointGeo(
                    item.path("lat").asDouble(),
                    item.path("lng").asDouble())
            );
        }
        return new LineStringGeo(ps.toArray(new PointGeo[size]));

        //下面这种方法解析后会导致其他错误比如数组只有3个, 但是解析出来变成了6个
        /*JsonToken token = jsonParser.getCurrentToken();
        String name = jsonParser.getCurrentName();
        ArrayList<PointGeo> ps = new ArrayList<>();
        if(token == JsonToken.START_ARRAY) {
            while(jsonParser.nextToken() == JsonToken.START_OBJECT) {
                ps.add(jsonParser.readValueAs(PointGeo.class));
            }
            token = jsonParser.nextToken();
        }
        return new LineStringGeo(ps.toArray(new PointGeo[ps.size()]));*/
    }
}
