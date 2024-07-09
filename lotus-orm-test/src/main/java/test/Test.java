package test;


import lotus.or.orm.geometry.model.PointGeo;

import java.util.Date;

public class Test {
    public int id;
    public String str;
    public Date createTime;
    public Integer typeId;
    public PointGeo p;

    public Test() {
    }

    public Test(int id, String str, Date createTime) {
        this.id = id;
        this.str = str;
        this.createTime = createTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public PointGeo getP() {
        return p;
    }

    public void setP(PointGeo p) {
        this.p = p;
    }
}
