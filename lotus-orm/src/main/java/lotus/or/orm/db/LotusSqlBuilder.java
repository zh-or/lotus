package lotus.or.orm.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LotusSqlBuilder {
    StringBuilder sql = new StringBuilder(255);

    ArrayList<WhereItem> wheres = new ArrayList<>(3);
    ArrayList<OrderItem> orders = new ArrayList<>(3);
    ArrayList<String> fields = new ArrayList<>(20);

    HashSet<String> defFields = new HashSet<>(10);

    int limitStart = -1;
    int limitSize = -1;
    String table;

    public LotusSqlBuilder(String table) {
        this.table = table;
    }

    public void setFields(List<String> fields) {
        this.fields.clear();
        this.fields.addAll(fields);
    }

    public void setSql(String sql) {
        this.sql.setLength(0);
        this.sql.append(sql);
    }

    public void addWhere(WhereItem item) {
        wheres.add(item);
    }

    public void addOrder(OrderItem item) {
        orders.add(item);
    }

    public void limit(int start, int size) {
        limitStart = start;
        limitSize = size;
    }

    public void addDefFields(String ...fs) {
        for(String f : fs) {
            defFields.add(f);
        }
    }

    public boolean isDefaultFields(String name) {
        return defFields.contains(name);
    }

    public String buildCount() {
        StringBuilder sb = new StringBuilder(255);
        sb.append("select count(`");
        sb.append(fields.get(0));
        sb.append("`) from ");
        sb.append(table);

        buildComSql(sb);
        return sb.toString();
    }

    public String buildSelect() {
        if(sql.length() > 0) {
            return sql.toString();
        }
        sql.append("select ");

        if(fields.size() > 0) {
            for(String f : fields) {
                sql.append(f);
                sql.append(",");
            }
            sql.setLength(sql.length() - 1);
        }

        sql.append(" from ");
        sql.append(table);
        sql.append(" ");
        buildComSql(sql);
        return sql.toString();
    }

    public String buildUpdate() {
        if(sql.length() > 0) {
            return sql.toString();
        }
        sql.append("update set");

        if(fields.size() > 0) {
            for(String f : fields) {
                sql.append(f);
                sql.append("=");
                if(isDefaultFields(f)) {
                    sql.append("default,");
                } else {
                    sql.append("?,");
                }
            }
            sql.setLength(sql.length() - 1);
        }

        buildComSql(sql);
        return sql.toString();
    }

    public String buildDelete() {
        if(sql.length() > 0) {
            return sql.toString();
        }
        sql.append("delete from ");
        sql.append(table);
        buildComSql(sql);
        return sql.toString();
    }

    public String buildInsert() {
        if(sql.length() > 0) {
            return sql.toString();
        }
        sql.append("insert into ");
        sql.append(table);

        if(fields.size() > 0) {
            sql.append("(");
            for(String f : fields) {
                sql.append("`");
                sql.append(f);
                sql.append("`");
                sql.append(",");
            }
            sql.setLength(sql.length() - 1);
            sql.append(") VALUES(");

            for(String f : fields) {
                if(isDefaultFields(f)) {
                    sql.append("default,");
                } else {
                    sql.append("?,");
                }
            }
            sql.setLength(sql.length() - 1);
            sql.append(")");
        }
        return sql.toString();
    }


    /**组装where, order by*/
    public void buildComSql(StringBuilder sb) {
        if(wheres.size() > 0) {
            sb.append(" where ");

            for(WhereItem wi : wheres) {
                if("m".equals(wi.m)) {
                    sb.append(wi.k);
                } else {
                    sb.append(wi.k)
                        .append(wi.m)
                        .append(wi.v);
                }
            }
        }

        if(orders.size() > 0) {
            sb.append(" order by ");
            for(OrderItem oi : orders) {
                if("m".equals(oi.m)) {
                    sb.append(oi.field);
                } else {
                    sb.append(oi.m)
                        .append(" ")
                        .append(oi.field).append(" ");
                }
            }
        }
        if(limitStart != -1 && limitSize != -1) {
            sb.append(" limit ");
            sb.append(limitStart);
            sb.append(", ");
            sb.append(limitSize);
        }
    }

}
