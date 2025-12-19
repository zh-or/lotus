package or.lotus.core.nio;

/** 解码完成后调用此方法的write写入消息对象 */
public class ProtocolDecoderOutput {
    private Object data = null;

    public void write(Object data) {
        this.data = data;
    }

    public Object read() {
        return this.data;
    }
}
