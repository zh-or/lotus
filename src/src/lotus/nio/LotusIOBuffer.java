package lotus.nio;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

public class LotusIOBuffer {
    private NioContext            context;
    private ArrayList<ByteBuffer> buffers;
    private ByteBuffer            curBuffer;
    
    public LotusIOBuffer(NioContext context) {
        buffers = new ArrayList<ByteBuffer>(2);
        this.context = context;
        allocBuffer();
    }

    public ByteBuffer getCurrenBuffer() {
        return curBuffer;
    }
    
    public ByteBuffer[] getAllBuffer() {
        ByteBuffer[] tBuffers = new ByteBuffer[this.buffers.size()];
        return this.buffers.toArray(tBuffers);
    }
    
    private void allocBuffer() {
        this.curBuffer = context.getByteBufferFormCache();
        this.buffers.add(curBuffer);
    }
    
    public void append(ByteBuffer buff) {
        if(this.curBuffer.position() > 0) {
            this.buffers.add(curBuffer);
        } else {
            this.context.putByteBufferToCache(this.curBuffer);
        }
        this.buffers.set(this.buffers.size() - 1, buff);
        this.curBuffer = buff;
    }

    public void append(byte[] src) {
        this.append(src, 0, src.length);
    }
    
    public void append(byte[] src, int offset, int length) {
        int lossLen = length, putLen, remaining;
        do {
            remaining = this.curBuffer.remaining();
            
            if(lossLen > remaining) {
                putLen = remaining;
            } else {
                putLen = lossLen;
            }
            lossLen = lossLen - putLen;
            this.curBuffer.put(src, offset, putLen);
            if(lossLen > 0) {
                offset = offset + putLen;
                allocBuffer();
            }
        } while(lossLen > 0);
    }
    
    public void append(byte b) {
        if(this.curBuffer.remaining() < 1) {
            this.allocBuffer();
        }
        this.curBuffer.put(b);
    }
    
    public boolean hasData() { 
        return this.curBuffer.position() > 0 || this.buffers.size() > 1;
    }
    
    public void free() {
        for(ByteBuffer buff : buffers) {
            //不缓存此类型
            if(!(buff instanceof MappedByteBuffer)) {
                context.putByteBufferToCache(buff);
            }
        }
        this.buffers.clear();
        this.curBuffer = null;
    }
}
