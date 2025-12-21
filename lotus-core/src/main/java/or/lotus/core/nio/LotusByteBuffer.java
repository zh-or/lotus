package or.lotus.core.nio;

import or.lotus.core.common.Utils;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
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
        this(context, isUseDirectBuffer, null);
    }

    public LotusByteBuffer(NioContext context, boolean isUseDirectBuffer, ByteBuffer buff) {
        int initialCapacity = context.pooledBufferStepCount;
        this.context = context;
        this.buffers = new ByteBuffer[initialCapacity];
        //默认先申请一个以免后面老是需要判断是否为空
        if(buff == null) {
            buff = context.getByteBufferFormCache(0, isUseDirectBuffer);
        }
        this.buffers[0] = buff;
        this.readIndex = 0;
        this.writeIndex = 0;
        this.isUseDirectBuffer = isUseDirectBuffer;
    }

    /** 切换到读模式 */
    public void flip() {
        for(int i = 0; i <= writeIndex; i++) {
            ByteBuffer buf = buffers[i];
            buf.flip();
        }
    }

    /** 移除已经读取过的数据,并把未读取的数据移动到前面, 转变为写模式 */
    public void compact() {
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
            //保证始终有一个可写的ByteBuffer在里面
            writeIndex = 0;
            buffers[0] = context.getByteBufferFormCache(0, isUseDirectBuffer);
        } else {
            if(endIndex > 0) {
                //大于0才需要复制
                System.arraycopy(buffers, endIndex, buffers, 0, writeIndex - endIndex + 1);
                writeIndex = writeIndex - endIndex;
            }
        }
        readIndex = 0;
    }

    public ByteBuffer[] getAllDataBuffer() {
        return getAllDataBuffer(false);
    }

    /** 获取后是否清空当前buffer, 清空后buffer由调用者管理生命周期 */
    public ByteBuffer[] getAllDataBuffer(boolean clearBuffer) {
        ByteBuffer[] dataBuffer = new ByteBuffer[writeIndex + 1];
        System.arraycopy(buffers, 0, dataBuffer, 0, writeIndex + 1);
        if(clearBuffer) {
            for(int i = 0; i < buffers.length; i++) {
                buffers[i] = null;
            }
            writeIndex = 0;
            readIndex = 0;
            buffers[0] = EMPTY_BUFFER;
        }
        return dataBuffer;
    }

    public static LotusByteBuffer mergeBuffer(LotusByteBuffer base, LotusByteBuffer buf) {
        return mergeBuffer(base, buf, buf.getDataLength());
    }

    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);//占位用

    /** 把buf合并到base, 如果base为空则创建一个新的buffer */
    public static LotusByteBuffer mergeBuffer(LotusByteBuffer base, LotusByteBuffer buf, long length) {
        LotusByteBuffer ret = null;

        if(base == null) {
            ret = new LotusByteBuffer(buf.context, buf.isUseDirectBuffer, EMPTY_BUFFER);
        } else {
            ret = base;
        }
        ByteBuffer[] buffers = buf.getAllDataBuffer();
        ByteBuffer t;
        long loss = length, remaining;
        for(int i = 0; i < buffers.length; i++) {
            t = buffers[i];
            remaining = t.remaining();
            if(loss < remaining) {
                //这里强制转换为int是没有问题的, 因为单个buffer大小限制为int范围内
                byte[] bytes = new byte[(int) loss];
                t.get(bytes, 0, (int) loss);
                ret.append(bytes, 0, (int) loss);
            } else {
                ret.append(buffers[i]);
                buf.buffers[i] = null;
            }
            loss += remaining;
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
            if(buff != null && !(buff instanceof MappedByteBuffer)) {
                context.putByteBufferToCache(buff);
            }
        }
        buffers = null;
        readIndex = 0;
        writeIndex = 0;

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
        ByteBuffer currentBuff = buffers[writeIndex];
        if(currentBuff.position() <= 0) {//无数据直接替换
            context.putByteBufferToCache(currentBuff);
        } else {
            checkAndExpansionBuffer();
            writeIndex ++;
        }
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
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].mark();
        }
    }

    @Override
    public void reset() {
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].reset();
        }
    }

    @Override
    public void rewind() {
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].rewind();
        }
        readIndex = 0;
    }

    @Override
    public void clear() {
        for(int i = 0; i <= writeIndex; i++) {
            buffers[i].clear();
        }
        readIndex = 0;
        writeIndex = 0;
    }

    @Override
    public byte get(int index) {
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
        int step = 0, bufStart = 0, bufEnd = 0;
        ByteBuffer buff;
        byte[] dst = null;

        int d, b, bLen = bytes.length, mid = 0;
        int matchStart = -1;

        for(int i = 0; i <= writeIndex; i++) {
            buff = buffers[i];
            if(buff.isDirect()) {
                bufStart = 0;
                bufEnd = buff.remaining();
                dst = new byte[bufEnd];
                buff.duplicate().get(dst);//相当于复制到堆内来了
            } else {
                dst = buff.array();
                bufStart = buff.arrayOffset();
                bufEnd = buff.remaining();
            }
            if(dst != null) {
                for(d = bufStart; d < bufEnd; d++) {
                    for(b = mid; b < bLen; b++) {
                        if(dst[d + b] == bytes[b]) {
                            mid = b;
                            if(matchStart == -1) {
                                matchStart = d + step;
                            }
                        } else {
                            matchStart = -1;
                            mid = 0;
                            break;
                        }
                    }
                    if(matchStart != -1 && mid == bLen - 1) {
                        break;
                    }
                }

                step += (bufEnd - bufStart);
            }
        }

        return matchStart;
    }
}
