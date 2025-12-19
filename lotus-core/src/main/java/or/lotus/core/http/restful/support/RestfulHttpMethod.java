package or.lotus.core.http.restful.support;

public enum RestfulHttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    REQUEST//其他请求

    ;

    public static RestfulHttpMethod byName(String type) {
        if(type == null) {
            return null;
        }
        for(RestfulHttpMethod m : RestfulHttpMethod.values()) {
            if(m.name().equalsIgnoreCase(type)) {
                return m;
            }
        }
        return null;
    }
}
