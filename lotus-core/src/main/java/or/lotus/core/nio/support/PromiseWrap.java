package or.lotus.core.nio.support;

public class PromiseWrap {
    private Promise promise;
    private Object data;

    public PromiseWrap(Promise promise, Object data) {
        this.promise = promise;
        this.data = data;
    }

    public Promise getPromise() {
        return promise;
    }

    public Object getData() {
        return data;
    }
}
