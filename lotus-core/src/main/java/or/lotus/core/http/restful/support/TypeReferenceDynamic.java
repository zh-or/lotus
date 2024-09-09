package or.lotus.core.http.restful.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class TypeReferenceDynamic<T> extends com.fasterxml.jackson.core.type.TypeReference<T> {
    private final Type type;
    private final Type wrapType;

    public TypeReferenceDynamic(Type wrapType, Type type) {
        super();
        this.type = type;
        this.wrapType = wrapType;
    }

    @Override
    public Type getType() {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{type};
            }

            @Override
            public Type getRawType() {
                return wrapType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

}
