package or.lotus.core.http.restful.support;

import java.lang.reflect.Type;

public class TypeReferenceDynamicList<T> extends com.fasterxml.jackson.core.type.TypeReference<T> {
    private final Type type;

    public TypeReferenceDynamicList(Type type) {
        super();
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

}
