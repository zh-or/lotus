package or.lotus.core.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import static java.lang.invoke.MethodType.methodType;


final class ByteBufferClear {
    public static final Logger log = LoggerFactory.getLogger(ByteBufferClear.class);


    private static MethodHandle clearMethod = null;

    /** 是否大于java8 */
    static boolean isGtJava8 = false;

    static {
        try {
            isGtJava8 = !System.getProperty("java.specification.version").startsWith("1.");
            if(isGtJava8) {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                Unsafe unsafe = (Unsafe) field.get(null);

                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle invokeCleaner = lookup.findVirtual(
                        unsafe.getClass(),
                        "invokeCleaner",
                        methodType(void.class, ByteBuffer.class)
                );
                invokeCleaner = invokeCleaner.bindTo(unsafe);
                invokeCleaner.invokeExact(ByteBuffer.allocateDirect(1));
                clearMethod = invokeCleaner;
            }
        } catch (Throwable e) {
            log.debug("ByteBufferClear 初始化出错:", e);
        }
    }

    public static void cleanDirectBuffer(ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return;
        }
        try {
            if(isGtJava8) {
                if(clearMethod != null) {
                    clearMethod.invokeExact(buffer);
                }
            } else {
                Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
                if (cleaner != null) {
                    cleaner.clean();
                }
            }
        } catch (Throwable e) {
            log.debug("当前无法释放DirectByteBuffer:", e);
        }
    }
}
