package or.lotus.core.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class EncodeOutByteBuffer {
    NioContext context;
    OutWrapper[] buffers;
    int writeIndex;
    public EncodeOutByteBuffer(NioContext context) {
        writeIndex = -1;
        buffers = new OutWrapper[context.pooledBufferStepCount];
        this.context = context;
    }

    public EncodeOutByteBuffer append(ByteBuffer[] buff) {
        for(ByteBuffer buf : buff) {
            append(buf);
        }
        return this;
    }

    public EncodeOutByteBuffer append(ByteBuffer buffer) {
        checkAndExpansionBuffer();
        writeIndex ++;
        buffers[writeIndex] = new OutWrapper(buffer);
        return this;
    }

    public EncodeOutByteBuffer append(byte b) {
        ByteBuffer buff = getCurrentWriteBuffer();
        buff.put(b);
        return this;
    }

    public EncodeOutByteBuffer append(byte[] src) {
        append(src, 0, src.length);
        return this;
    }

    public EncodeOutByteBuffer append(byte[] src, int offset, int length) {
        int len = length;
        if(len > 0) {
            ByteBuffer buff;
            int remaining, pos = offset, size;
            do {
                buff = getCurrentWriteBuffer();
                remaining = buff.remaining();
                size = Math.min(remaining, len - pos);
                buff.put(src, pos, size);
                pos += size;
            } while(pos < len);
        }
        return this;
    }

    /**
     * 已0拷贝的方式发送文件, 发送完毕会自动调用FileChannel.close()
     * */
    public EncodeOutByteBuffer append(FileChannel channel, long pos, long size) {

        checkAndExpansionBuffer();
        writeIndex ++;
        buffers[writeIndex] = new OutWrapper(channel, pos, size);
        return this;
    }

    public ByteBuffer getCurrentWriteBuffer() {
        if(writeIndex == -1) {
            writeIndex++;
            buffers[writeIndex] = new OutWrapper(context.getByteBufferFormCache(0, context.isUseDirectBuffer));
            return buffers[writeIndex].buffer;
        }
        OutWrapper buff = buffers[writeIndex];
        if(buff.isBuffer && buff.buffer.hasRemaining()) {
            return buff.buffer;
        }

        writeIndex++;
        buffers[writeIndex] = new OutWrapper(context.getByteBufferFormCache(0, context.isUseDirectBuffer));
        return buffers[writeIndex].buffer;

    }

    protected void checkAndExpansionBuffer() {
        int newIndex = writeIndex + 1;
        if(newIndex >= buffers.length) {
            OutWrapper[] newBuffers = new OutWrapper[buffers.length + context.pooledBufferStepCount];
            System.arraycopy(buffers, 0, newBuffers, 0, newIndex);
            buffers = newBuffers;
        }
    }

    public OutWrapper[] getAllDataBuffer() {
        if(writeIndex == -1) {
            return new OutWrapper[0];
        }
        OutWrapper[] dataBuffer = new OutWrapper[writeIndex + 1];
        System.arraycopy(buffers, 0, dataBuffer, 0, writeIndex + 1);
        return dataBuffer;
    }

    public void release() {
        for(int i = 0; i <= writeIndex; i++) {
            OutWrapper item = buffers[i];
            if(item != null) {
                item.release();
            }
            buffers[i] = null;
        }
        writeIndex = -1;
    }

    public class OutWrapper {
        public boolean isBuffer;

        public ByteBuffer buffer;

        public FileChannel fileChannel;
        public long pos;
        public long size;

        public OutWrapper(ByteBuffer buffer) {
            this.buffer = buffer;
            isBuffer = true;
        }


        public OutWrapper(FileChannel fileChannel, long pos, long size) {
            this.fileChannel = fileChannel;
            this.pos = pos;
            this.size = size;
            isBuffer = false;
        }

        public void release() {
            if(isBuffer) {
                context.putByteBufferToCache(buffer);
                buffer = null;
            } else {
                try {
                    if(fileChannel != null) {
                        fileChannel.close();
                        fileChannel = null;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
