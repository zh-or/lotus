package test;


import java.util.Date;

public class Test {
    public int id;
    public String str;
    public Date createTime;
    public int typeId;

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

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }
}
