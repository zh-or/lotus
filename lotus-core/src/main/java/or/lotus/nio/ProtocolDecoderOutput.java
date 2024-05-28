package or.lotus.nio;

public class ProtocolDecoderOutput {
    private Object data = null;

    public void write(Object data){
        this.data = data;
    }

    public Object read(){
        return this.data;
    }
}
