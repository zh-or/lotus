package lotus.or.orm.db;

public class WhereItem {
    public String k;
    public String m;
    public String v;


    public static WhereItem eq(String k, String v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "=";
        return obj;
    }


    public static WhereItem lt(String k, String v) {
        WhereItem obj = new WhereItem();
        obj.k = k;
        obj.v = v;
        obj.m = "<";
        return obj;
    }

    public static WhereItem gt(String k, String v) {
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
