package or.lotus.mybatis.types;

public enum ActionType {
    WATCH(0, "查看"),
    UP(1, "赞"),
    DOWN(2, "踩"),
    PUBLISH_COMMENT(3, "发布留言"),
    ;
    public int code;
    public String name;

    ActionType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ActionType valueOf(int code) {
        for (ActionType type : ActionType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
