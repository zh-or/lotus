package or.lotus.core.nio;

public class LotusPooledByteBuffer {
    private int maxBufferCount;//最大缓存buffer数量
    private int bufferDataSize;//默认分配buffer大小

    public LotusPooledByteBuffer(int maxBufferCount, int bufferDataSize) {
        this.maxBufferCount = maxBufferCount;
        this.bufferDataSize = bufferDataSize;
    }


}
