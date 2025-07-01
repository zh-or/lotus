package or.lotus.orm.db;

public class InertQueueObj {
    public Object obj;
    public InsertQueueCallback callback;

    public InertQueueObj(Object obj) {
        this.obj = obj;
    }

    public InertQueueObj(Object obj, InsertQueueCallback callback) {
        this.obj = obj;
        this.callback = callback;
    }

    public class InsertQueueCallback {
        public void success(Object obj) {}
        public void fail(Exception e) {}
    }
}
