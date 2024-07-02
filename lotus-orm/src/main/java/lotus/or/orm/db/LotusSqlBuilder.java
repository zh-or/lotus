package lotus.or.orm.db;

import java.util.ArrayList;
import java.util.List;

public class LotusSqlBuilder {
    StringBuilder sql = new StringBuilder(255);

    ArrayList<WhereItem> wheres = new ArrayList<>(3);
    ArrayList<OrderItem> orders = new ArrayList<>(3);

    ArrayList<String> fields = new ArrayList<>(20);

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
        buildComSql();
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
                sql.append("?,");
            }
            sql.setLength(sql.length() - 1);
        }

        buildComSql();
        return sql.toString();
    }

    public String buildDelete() {
        if(sql.length() > 0) {
            return sql.toString();
        }
        sql.append("delete from ");
        sql.append(table);
        buildComSql();
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
                sql.append("?,");
            }
            sql.setLength(sql.length() - 1);
            sql.append(")");
        }
        return sql.toString();
    }


    /**组装where, order by*/
    public void buildComSql() {
        if(wheres.size() > 0) {
            sql.append(" where ");

            for(WhereItem wi : wheres) {
                if("m".equals(wi.m)) {
                    sql.append(wi.k);
                } else {
                    sql.append(wi.k)
                            .append(wi.m)
                            .append(wi.v);
                }
            }
        }

        if(orders.size() > 0) {
            sql.append(" order by ");
            for(OrderItem oi : orders) {
                if("m".equals(oi.m)) {
                    sql.append(oi.field);
                } else {
                    sql.append(oi.m)
                        .append(" ")
                        .append(oi.field).append(" ");
                }
            }
        }

    }

}
