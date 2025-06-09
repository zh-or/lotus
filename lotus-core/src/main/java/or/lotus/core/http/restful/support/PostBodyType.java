package or.lotus.core.http.restful.support;

import or.lotus.core.common.Utils;

public enum PostBodyType {
    URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART("multipart/form-data"),
    JSON("application/json"),
    TEXT("text/plain"),
    XML("application/xml");

    private String type;

    PostBodyType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public static PostBodyType getByType(String type) {
        if(!Utils.CheckNull(type)) {
            //application/json; charset=utf-8
            int p = type.indexOf(";");
            if(p > 0) {
                type = type.substring(0, p);
            }
        }


        for (PostBodyType t : PostBodyType.values()) {
            if (t.type().equals(type)) {
                return t;
            }
        }
        return null;
    }
}
