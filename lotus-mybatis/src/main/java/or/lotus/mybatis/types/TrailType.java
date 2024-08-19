package or.lotus.mybatis.types;

public enum TrailType {
    BIKE_GO(1, "骑行"),
    TRAIL(2, "徒步"),
    TWEET(3, "散步"),
    BIKE(5, "摩托车"),
    CAR(6, "汽车"),
            ;
    public int code;
    public String name;

    TrailType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TrailType valueOf(int code) {
        for (TrailType type : TrailType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
