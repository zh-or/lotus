package lotus.nio;

public abstract class IoProcess{
    protected NioContext                  context    	   = null;
    protected volatile boolean            isrun       	   = false;
    protected volatile int                isessiontimeout  = 0;

	public IoProcess(NioContext context){
		this.context = context;
        this.isessiontimeout = context.getSessionIdleTimeOut();
        this.isrun = true;
	}
}
