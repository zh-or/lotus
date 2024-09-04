package or.lotus.core.http.restful.support;

public enum PostBodyType {
    URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART("multipart/form-data"),
    JSON("application/json"),
    XML("application/xml");

    private String type;

    PostBodyType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public static PostBodyType getByType(String type) {
        for (PostBodyType t : PostBodyType.values()) {
            if (t.type().equals(type)) {
                return t;
            }
        }
        return null;
    }
}
