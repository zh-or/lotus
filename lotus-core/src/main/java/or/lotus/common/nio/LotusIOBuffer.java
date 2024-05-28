package or.lotus.common.nio;

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

    /**
     * 从LotusIOBuffer复制
     * @param buffer 复制完成后此参数的buffer将被清空
     */
    public void copyFromBuffer(LotusIOBuffer buffer) {
        for(ByteBuffer buf: buffer.buffers) {
            append(buf);
        }
        buffer.buffers.clear();
        buffer.curBuffer = null;
    }

    public ByteBuffer getCurrentBuffer() {
        return curBuffer;
    }

    public ByteBuffer[] getAllBuffer() {
        ByteBuffer[] tBuffers = new ByteBuffer[buffers.size()];
        return buffers.toArray(tBuffers);
    }

    public int getDataLength() {
        int len = 0;
        for(ByteBuffer buf : buffers) {
            len += (buf.capacity() - buf.remaining());
        }
        return len;
    }

    public void allocBuffer() {
        curBuffer = context.getByteBufferFormCache();
        buffers.add(curBuffer);
    }

    public void append(ByteBuffer buff) {
        if(curBuffer.position() > 0) {
            buffers.add(curBuffer);
        } else {
            context.putByteBufferToCache(curBuffer);
        }
        buffers.set(buffers.size() - 1, buff);
        curBuffer = buff;
    }

    public void append(byte[] src) {
        append(src, 0, src.length);
    }

    public void append(byte[] src, int offset, int length) {
        int lossLen = length, putLen, remaining;
        do {
            remaining = curBuffer.remaining();

            if(lossLen > remaining) {
                putLen = remaining;
            } else {
                putLen = lossLen;
            }
            lossLen = lossLen - putLen;
            curBuffer.put(src, offset, putLen);
            if(lossLen > 0) {
                offset = offset + putLen;
                allocBuffer();
            }
        } while(lossLen > 0);
    }

    public void append(byte b) {
        if(curBuffer.remaining() < 1) {
            allocBuffer();
        }
        curBuffer.put(b);
    }

    public boolean hasData() {
        return curBuffer.position() > 0 || buffers.size() > 1;
    }

    public void free() {
        for(ByteBuffer buff : buffers) {
            //不缓存此类型
            if(!(buff instanceof MappedByteBuffer)) {
                context.putByteBufferToCache(buff);
            }
        }
        buffers.clear();
        curBuffer = null;
    }
}
