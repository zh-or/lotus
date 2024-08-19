package or.lotus.mybatis.types;

public enum OpenType {
    PUBLIC(0, "公开"),
    FRIEND(1, "好友"),
    PRIVATE(2, "私有"),
    ;
    public int code;
    public String name;

    OpenType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static OpenType valueOf(int code) {
        for (OpenType type : OpenType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
