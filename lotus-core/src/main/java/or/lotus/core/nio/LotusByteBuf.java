package or.lotus.core.nio;

import java.nio.ByteBuffer;

/** 不支持多线程, 支持release时释放append的MappedByteBuffer */
public interface LotusByteBuf {

    public boolean release();

    public void retain();

    /** 用于检查内存泄漏, 加个名字方便查找 */
    public void retain(String name);
    public void append(ByteBuffer[] buff);
    public void append(ByteBuffer buff);
    public void append(byte[] src);
    public void append(byte[] src, int offset, int length);
    public void append(byte b);

    /** 设置byte, 此方法不改变写指针 */
    public void set(int index, byte b);

    /** 当前已写入数据长度 */
    public int getDataLength();
    /** 标记当前读位置 */
    public void mark();
    /** 重置到标记的读位置 */
    public void reset();
    /** 重置读指针 */
    public void rewind();

    public void clear();

    /** 读取byte, 此方法不改变读指针 */
    public byte get(int index);
    public int get(byte[] src);
    public int get(byte[] dst, int dstOffset, int length);
    public byte get();

    public int search(byte[] bytes);

    /** 搜索指定子串的位置如果找到则返回首个位置, 未搜索到返回-1, 不需要调用mark, reset
     * 对于DirectByteBuffer来说 此方法的效率并不好.
     * */
    public int search(int start, byte[] bytes);
}
