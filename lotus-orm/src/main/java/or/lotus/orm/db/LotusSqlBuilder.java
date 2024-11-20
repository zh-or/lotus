package or.lotus.orm.db;

import or.lotus.core.intmap.SparseArray;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LotusSqlBuilder {
    StringBuilder sql = new StringBuilder(4096);//4k

    ArrayList<WhereItem> wheres = new ArrayList<>(3);
    ArrayList<OrderItem> orders = new ArrayList<>(3);
    ArrayList<String> fields = new ArrayList<>(20);
    HashSet<String> defFields = new HashSet<>(10);

    /**key = params的下标(不算默认值)*/
    SparseArray<String> sqlHolder = new SparseArray<>(16);

    int limitStart = -1;
    int limitSize = -1;
    String table;
    Database db;

    public LotusSqlBuilder(Database db, String table) {
        this.table = table;
        this.db = db;
    }

    public void setFields(List<String> fields) {
        this.fields.clear();
        this.fields.addAll(fields);
    }

    public void setSql(String sql) {
        this.sql.setLength(0);
        this.sql.append(sql);
    }

    public void addHolder(int key, String value) {
        sqlHolder.put(key, value);
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

    /**生成sql时只会添加where条件*/
    public String buildCount() throws SQLException {
        return buildCount(this.table);
    }

    public String buildCount(String table) throws SQLException {
        return buildCount(db.getConfig().getPrimaryKeyName(), table);
    }


    /**生成sql时只会添加where条件*/
    public String buildCount(String countField, String table) throws SQLException {
        StringBuilder sb = new StringBuilder(255);
        sb.append("select count(");
        sb.append(countField);
        sb.append(") from ");
        sb.append(table);

        buildWhere(sb);
        return sb.toString();
    }

    public String buildSelect() throws SQLException {
        if(sql.length() > 0) {
            buildComSql(sql);
            return sql.toString();
        }
        sql.append("select ");

        if(fields.size() > 0) {
            for(String f : fields) {
                sql.append("`");
                sql.append(f);
                sql.append("`");
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

    public String buildUpdate() throws SQLException {
        if(sql.length() > 0) {
            buildComSql(sql);
            return sql.toString();
        }
        sql.append("update `");
        sql.append(table);
        sql.append("` set ");
        int size = fields.size();

        if(size > 0) {
            int i = 0;
            for(String f: fields) {
                sql.append("`");
                sql.append(f);
                sql.append("`");
                sql.append("=");

                if(isDefaultFields(f)) {
                    sql.append("default,");
                } else {
                    String holder = sqlHolder.get(i);
                    if(holder != null) {
                        sql.append(holder).append(",");
                    } else {
                        sql.append("?,");
                    }
                    i++;
                }
            }
            sql.setLength(sql.length() - 1);
        }
        buildComSql(sql);
        return sql.toString();
    }

    public String buildDelete() throws SQLException {
        if(sql.length() > 0) {
            buildComSql(sql);
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
        int size = fields.size();
        if(size > 0) {
            sql.append("(");
            for(String f : fields) {
                sql.append("`");
                sql.append(f);
                sql.append("`");
                sql.append(",");
            }
            sql.setLength(sql.length() - 1);
            sql.append(") VALUES(");
            int i = 0;
            for(String f : fields) {
                if(isDefaultFields(f)) {
                    sql.append("default,");
                } else {
                    String holder = sqlHolder.get(i);
                    if(holder != null) {
                        sql.append(holder).append(",");
                    } else {
                        sql.append("?,");
                    }
                    i++;
                }
            }
            sql.setLength(sql.length() - 1);
            sql.append(")");
        }
        return sql.toString();
    }

    public void buildWhere(StringBuilder sb) throws SQLException {
        if(wheres.size() > 0) {
            int needCd = 0;
            sb.append(" where ");
            for(WhereItem wi : wheres) {
                if("m".equals(wi.m)) {
                    sb.append(wi.k).append(" ");
                } else if("in".equals(wi.m)) {
                    if(needCd % 2 != 0) {
                        throw new SQLException("where 之间需要增加条件");
                    }
                    sb.append('`').append(wi.k).append('`')
                            .append(" in(").append(wi.v).append(") ");
                } else {
                    if(needCd % 2 != 0) {
                        throw new SQLException("where 之间需要增加条件");
                    }
                    sb.append('`').append(wi.k).append('`')
                            .append(" ")
                            .append(wi.m).append(" ")
                            .append(wi.v).append(" ");
                }
                needCd++;
            }
        }

    }


    /**组装where, order by*/
    public void buildComSql(StringBuilder sb) throws SQLException {
        buildWhere(sb);

        if(orders.size() > 0) {
            sb.append(" order by ");
            for(OrderItem oi : orders) {
                if("m".equals(oi.m)) {
                    sb.append('`').append(oi.field).append('`').append(" ");
                } else {
                    sb.append('`').append(oi.field).append('`')
                        .append(" ")
                        .append(oi.m).append(" ");
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
