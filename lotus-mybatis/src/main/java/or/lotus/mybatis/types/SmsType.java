package or.lotus.mybatis.types;

public enum SmsType {
    LOGIN("login", "登录"),
    BIND_PHONE("bind_phone", "微信账号绑定web账号"),
            ;
    public String code;
    public String name;

    SmsType(String code, String name) {
        this.code = code;
        this.name = name;
    }


    public static SmsType getByCode(String code) {
        for (SmsType type : SmsType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
