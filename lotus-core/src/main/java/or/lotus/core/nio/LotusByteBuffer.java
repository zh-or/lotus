package or.lotus.core.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class LotusByteBuffer implements LotusByteBuf {
    private final NioContext context;
    private boolean isUseDirectBuffer = false;
    private int useCount = 1;
    private ByteBuffer[] buffers;
    private int readIndex;
    private int writeIndex;


    public LotusByteBuffer(NioContext context, boolean isUseDirectBuffer) {
        int initialCapacity = context.pooledBufferStepCount;
        this.context = context;
        this.buffers = new ByteBuffer[initialCapacity];
        this.readIndex = 0;
        this.writeIndex = -1;
        this.isUseDirectBuffer = isUseDirectBuffer;
    }

    public boolean isEmpty() {
        return writeIndex == -1;
    }

    /** 切换到读模式 */
    public void flip() {
        if(writeIndex == -1) {
            return;
        }
        for(int i = 0; i <= writeIndex; i++) {
            ByteBuffer buf = buffers[i];
            buf.flip();
        }
    }

    /** 移除已经读取过的数据,并把未读取的数据移动到前面, 转变为写模式 */
    public void compact() {
        if(writeIndex == -1) {
            return;
        }
        int endIndex = -1;

        for(int i = 0; i <= writeIndex; i ++) {
            ByteBuffer buff = buffers[i];
            if(buff != null) {
                buff.compact();
                if(buff.position() > 0) {
                    if(endIndex == -1) {
                        endIndex = i;
                    }
                } else {
                    context.putByteBufferToCache(buff);
                    buffers[i] = null;
                }
            }
        }

        if(endIndex == -1) {
            //xxx保证始终有一个可写的ByteBuffer在里面xxx
            //当使用大缓存时, 会导致每个session都占用一个 ByteBuffer 导致每个链接即使无数据交互也占用内存, 对高并发不友好
            //writeIndex = 0;
            //buffers[0] = context.getByteBufferFormCache(0, isUseDirectBuffer);
            writeIndex = -1;
        } else {
            if(endIndex > 0) {
                //大于0才需要复制
                System.arraycopy(buffers, endIndex, buffers, 0, writeIndex - endIndex + 1);
                writeIndex = writeIndex - endIndex;
            }
        }
        readIndex = 0;
    }

    /** 写入length长度的数据到fileChannel中, 最大不超过当前buffer的长度, 会移动读写位置
     * 如果不是追加模式打开的FileChannel, 需要自己控制文件的position
     * @return 返回实际写入长度
     * */
    public long writeToFile(FileChannel fileChannel, long length, long timeout) throws IOException {
        long loss = Math.min(length, getDataLength());
        long start = System.currentTimeMillis();
        if(loss > 0) {
            int remaining, i;
            for(i = readIndex; i <= writeIndex; i++) {
                ByteBuffer t = buffers[i];
                readIndex = i;
                while((remaining = t.remaining()) > 0 && loss > 0) {
                    if(loss > remaining) {
                        loss -= fileChannel.write(t);
                    } else {
                        int limit = t.limit();
                        t.limit(t.position() + (int) loss);
                        loss -= fileChannel.write(t);
                        t.limit(limit);
                    }
                    if(System.currentTimeMillis() - start > timeout) {
                        throw new IOException("请检查所设置的临时目录是否已满, 请求body写入磁盘缓存时超时 > " + timeout + "ms");
                    }
                }
                if(loss <= 0) {
                    break;
                }
            }
        }
        return length - loss;
    }

    public ByteBuffer[] getAllDataBuffer() {
        return getAllDataBuffer(false);
    }

    /** 获取后是否清空当前buffer, 清空后buffer由调用者管理生命周期 */
    public ByteBuffer[] getAllDataBuffer(boolean clearBuffer) {
        if(writeIndex == -1) {
            return new ByteBuffer[0];
        }
        ByteBuffer[] dataBuffer = new ByteBuffer[writeIndex + 1];
        System.arraycopy(buffers, 0, dataBuffer, 0, writeIndex + 1);
        if(clearBuffer) {
            for(int i = 0; i <= writeIndex; i++) {
                buffers[i] = null;
            }
            writeIndex = -1;
            readIndex = 0;
        }
        return dataBuffer;
    }

    public static LotusByteBuffer mergeBuffer(LotusByteBuffer base, LotusByteBuffer buf) {
        return mergeBuffer(base, buf, buf.getDataLength());
    }

    /** 把buf合并到base, 如果base为空则创建一个新的buffer */
    public static LotusByteBuffer mergeBuffer(LotusByteBuffer base, LotusByteBuffer buf, long length) {
        LotusByteBuffer ret = null;

        if(base == null) {
            ret = new LotusByteBuffer(buf.context, buf.isUseDirectBuffer);
        } else {
            ret = base;
        }
        ByteBuffer[] buffers = buf.getAllDataBuffer(true);
        ByteBuffer t;
        long loss = length, remaining;
        for(int i = 0; i < buffers.length; i++) {
            t = buffers[i];
            remaining = t.remaining();
            if(loss < remaining) {
                //需要处理的是 有时候只需要 buf 中的一部分数据合并到 base
                //这里强制转换为int是没有问题的, 因为单个buffer大小限制为int范围内
                byte[] bytes = new byte[(int) loss];
                t.get(bytes, 0, (int) loss);
                ret.append(bytes, 0, (int) loss);
            } else {
                ret.append(buffers[i]);
            }
            loss -= remaining;
        }
        return ret;
    }

    @Override
    public boolean release() {
        useCount --;
        if(useCount > 0) {
            return false;
        }
        // 释放所有ByteBuffer回缓存池
        for (int i = 0; i <= writeIndex; i++) {
            ByteBuffer buff = buffers[i];
            if(buff != null) {
                context.putByteBufferToCache(buff);
                buffers[i] = null;
            }
        }
        buffers = null;
        readIndex = 0;
        writeIndex = -1;

        if(retainName != null) {
            context.retainMap.remove(this);
        }
        return true;
    }

    @Override
    public void retain() {
        useCount ++;
    }

    List<String> retainName = null;
    @Override
    public  void retain(String name) {
        useCount ++;
        if(retainName == null) {
            retainName = new ArrayList<>(1);
        }
        retainName.add(name);
        context.retainMap.put(this, retainName);
    }


    /** 获取一个能写的 ByteBuffer */
    public ByteBuffer getCurrentWriteBuffer() {
        if(writeIndex == -1) {
            writeIndex++;
            buffers[writeIndex] = context.getByteBufferFormCache(0, isUseDirectBuffer);
            return buffers[writeIndex];
        }
        ByteBuffer buff = buffers[writeIndex];
        if(!buff.hasRemaining()) {
            checkAndExpansionBuffer();
            writeIndex++;
            buff = context.getByteBufferFormCache(0, isUseDirectBuffer);
            buffers[writeIndex] = buff;
        }
        return buff;
    }


    protected void checkAndExpansionBuffer() {
        int newIndex = writeIndex + 1;
        if(newIndex >= buffers.length) {
            ByteBuffer[] newBuffers = new ByteBuffer[buffers.length + context.pooledBufferStepCount];
            System.arraycopy(buffers, 0, newBuffers, 0, newIndex);
            buffers = newBuffers;
        }
    }

    @Override
    public void append(ByteBuffer[] buff) {
        for(ByteBuffer buf : buff) {
            append(buf);
        }
    }

    @Override
    public void append(ByteBuffer buff) {
        checkAndExpansionBuffer();
        writeIndex ++;
        buffers[writeIndex] = buff;
    }

    @Override
    public void append(byte[] src) {
        append(src, 0, src.length);
    }

    @Override
    public void append(byte[] src, int offset, int length) {
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
    }

    @Override
    public void append(byte b) {
        ByteBuffer buff = getCurrentWriteBuffer();
        buff.put(b);
    }

    @Override
    public void set(int index, byte b) {
        ByteBuffer buff;
        int size = 0, pos;
        for(int i = 0; i <= writeIndex; i++) {
            buff = buffers[i];
            pos = buff.position();

            if(index <= (size + pos)) {
                buff.put(index - size, b);
                return;
            }
            size += pos;
        }
    }

    /** 写模式下读取当前写入数据的数量 */
    public int getCountPosition() {
        if(writeIndex == -1) {
            return 0;
        }
        int size = 0;
        ByteBuffer buff;
        for(int i = 0; i <= writeIndex; i++) {
            buff = buffers[i];
            size += buff.position();
        }
        return size;
    }

    /** 读模式为返回可读取数据总长度, 写模式时返回值无效 */
    @Override
    public int getDataLength() {
        if(writeIndex == -1) {
            return 0;
        }
        int size = 0;
        ByteBuffer buff;
        for(int i = 0; i <= writeIndex; i++) {
            buff = buffers[i];
            size += buff.remaining();
        }
        return size;
    }

    @Override
    public void mark() {
        if(writeIndex == -1) {
            return;
        }
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].mark();
        }
    }

    @Override
    public void reset() {
        if (writeIndex == -1) {
            return;
        }
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].reset();
        }
    }

    @Override
    public void rewind() {
        if (writeIndex == -1) {
            return;
        }
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].rewind();
        }
        readIndex = 0;
    }

    @Override
    public void clear() {
        if (writeIndex == -1) {
            return;
        }
        for(int i = 0; i <= writeIndex; i++) {
            context.putByteBufferToCache(buffers[i]);
            buffers[i] = null;
        }
        readIndex = 0;
        writeIndex = -1;
    }

    @Override
    public byte get(int index) {
        if (writeIndex == -1) {
            throw new IndexOutOfBoundsException("index > dataLength");
        }
        ByteBuffer buff;
        int size = 0, pos;
        for(int i = 0; i <= writeIndex; i++) {
            buff = buffers[i];
            pos = buff.position();

            if(index <= (size + pos)) {
                return buff.get(index - size);
            }
            size += pos;
        }
        throw new IndexOutOfBoundsException("index > dataLength");
    }

    @Override
    public int get(byte[] src) {
        return get(src, 0, src.length);
    }

    @Override
    public int get(byte[] dst, int dstOffset, int length) {
        if(writeIndex == -1) {
            return 0;
        }
        ByteBuffer buff;
        int pos = dstOffset, size, loss = length;
        do {
            buff = buffers[readIndex];
            size = Math.min(loss, buff.remaining());
            buff.get(dst, pos, size);
            loss = loss - size;
            //读取完毕或者读指针已经到末尾
            if(loss <= 0 || readIndex == writeIndex) {
                break;
            }
            readIndex++;
            pos += size;
        } while(true);
        return length - loss;
    }

    @Override
    public byte get() {
        if(writeIndex == -1) {
            throw new IndexOutOfBoundsException("no data can read!");
        }

        ByteBuffer buff;
        do {
            buff = buffers[readIndex];
            if(buff == null) {
                System.out.println("buff == null");
            }
            if(buff.hasRemaining()) {
                return buff.get();
            }
            //读取完毕或者读指针已经到末尾
            if(readIndex == writeIndex) {
                break;
            }
            readIndex++;
        } while(true);
        throw new IndexOutOfBoundsException("no data can read!");
    }

    @Override
    public int search(byte[] bytes) {
        return search(0, bytes);
    }

    /** 朴素搜索 */
    @Override
    public int search(int start, byte[] pattern) {
        if (writeIndex == -1 || pattern == null || pattern.length == 0) {
            return -1;
        }
        ByteBuffer buffer;
        int patternLength = pattern.length;
        int i = 0, remaining, globalPos = 0, bufferPos, bufferLimit, patternIndex = 0;
        for(;i <= writeIndex; i++) {
            buffer = buffers[i];
            remaining = buffer.remaining();
            globalPos += remaining;
            if(globalPos <  start) {
                continue;
            }
            bufferPos = buffer.position();
            bufferLimit = buffer.limit();
            for(; bufferPos < bufferLimit; bufferPos++) {
                for(; patternIndex < patternLength; patternIndex++) {
                    if(buffer.get(bufferPos + patternIndex) != pattern[patternIndex]) {
                        patternIndex = 0;
                        break;
                    }
                }
                if(patternIndex >= patternLength) {
                    return globalPos - (bufferLimit - bufferPos);
                }
            }
        }
        return -1;
    }
}
