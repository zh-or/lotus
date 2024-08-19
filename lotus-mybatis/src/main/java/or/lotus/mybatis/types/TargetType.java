package or.lotus.mybatis.types;

public enum TargetType {
    TRAVEL_PLAN_TEMP(0, "路书草稿"),
    TRAVEL_PLAN(1, "路书"),
    TRAIL(2, "轨迹"),
    TWEET(3, "动态"),
    COMMENT(4, "留言"),
    HEAD_PIC(5, "头像"),
    TRAILS_THUMBNAIL(6, "轨迹缩略图"),
            ;
    public int code;
    public String name;

    TargetType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TargetType valueOf(int code) {
        for (TargetType type : TargetType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
