package or.lotus.orm.db;

public class WhereItem {
    public Object k;
    public String m;
    public Object v;


    public static WhereItem in(Object k, Object v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "in";
        return obj;
    }

    public static WhereItem like(Object k, Object v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "like";
        return obj;
    }

    public static WhereItem isNULL(Object k) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = "null";
        obj.m = "is";
        return obj;
    }

    public static WhereItem isNotNULL(Object k) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = "null";
        obj.m = "is not";
        return obj;
    }

    public static WhereItem not(Object k, Object v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "!=";
        return obj;
    }

    public static WhereItem eq(Object k, Object v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "=";
        return obj;
    }


    public static WhereItem lt(Object k, Object v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "<";
        return obj;
    }

    public static WhereItem gt(Object k, Object v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = ">";
        return obj;
    }

    public static WhereItem or() {
        WhereItem obj = new WhereItem();
        obj.m = "m";
        obj.k = "or";
        return obj;
    }


    public static WhereItem and() {
        WhereItem obj = new WhereItem();
        obj.m = "m";
        obj.k = "and";
        return obj;
    }

}
