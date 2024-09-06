package or.lotus.core.nio.support;

public class ProtocolDecoderOutput {
    private Object data = null;

    public void write(Object data){
        this.data = data;
    }

    public Object read(){
        return this.data;
    }
}
