package lotus.nio;

public abstract class IoProcess{
    protected NioContext                  context    	   = null;
    protected volatile boolean            brun       	   = false;
    protected ProtocolCodec               codec      	   = null;
    protected volatile int                isessiontimeout  = 0;

	public IoProcess(NioContext context){
		this.context = context;
        this.codec = context.getProtocoCodec();
        this.isessiontimeout = context.getSessionIdleTimeOut();
        this.brun = true;
	}
}
