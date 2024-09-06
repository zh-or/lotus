package or.lotus.core.nio.support;

public class Promise {
    private PromiseRunner then;
    private PromiseRunner exception;
    private PromiseRunner complete;

    public void then(PromiseRunner run) {
        then = run;
    }

    public void exception(PromiseRunner run) {
        exception = run;
    }

    public void complete(PromiseRunner run) {
        complete = run;
    }

    public void callThen(Object params) {
        try {
            if(then != null) {
                then.run(params);
                return;
            }
            if(complete != null) {
                complete.run(params);
            }
        } catch (Throwable e) {
            callException(e);
        }
    }
    public void callException(Throwable e) {
        if(exception != null) {
            exception.run(e);
        }
        if(complete != null) {
            complete.run(null);
        }
    }

    public interface PromiseRunner {
        void run(Object params);
    }
}
