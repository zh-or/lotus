package or.lotus.core.nio;

public abstract class IoProcess{
    protected NioContext                  context    	   = null;
    protected volatile boolean            isrun       	   = false;

	public IoProcess(NioContext context){
		this.context = context;
        this.isrun = true;
	}
}
