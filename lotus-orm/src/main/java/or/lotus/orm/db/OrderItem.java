package or.lotus.orm.db;

/**
 * 排序
 */
public class OrderItem {
    String m;
    String field;

    public static OrderItem desc(String field) {
        OrderItem o = new OrderItem();
        o.m = "desc";
        o.field = field;
        return o;
    }

    public static OrderItem asc(String field) {
        OrderItem o = new OrderItem();
        o.m = "asc";
        o.field = field;
        return o;
    }

    /**
     * OrderItem.orderBy("id desc, user_id asc")
     * */
    public static OrderItem orderBy(String orderBy) {
        OrderItem o = new OrderItem();
        o.m = "m";
        o.field = orderBy;
        return o;
    }
}
