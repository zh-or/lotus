package lotus.mq;

public abstract class IMQBase {
    private long timeout = 10000;
    public long getTimeOut(){return timeout;}
    public void setTimeOut(long t){this.timeout = t;}
    public abstract boolean equals(Object o);
}
