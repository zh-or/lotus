package or.lotus.core.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

final class ByteBufferClear {
    public static final Logger log = LoggerFactory.getLogger(ByteBufferClear.class);

    /** Java 9+ 策略: Unsafe.invokeCleaner(ByteBuffer) */
    private static Object theUnsafe;
    private static Method invokeCleanerMethod;

    /** Java 8 策略: DirectBuffer.cleaner().clean() */
    private static Field cleanerField;
    private static Method cleanMethod;

    static {
        boolean java9Plus = false;
        try {
            String version = System.getProperty("java.specification.version");
            java9Plus = !version.startsWith("1.");
        } catch (Exception e) {
            log.debug("ByteBufferClear 获取Java版本出错:", e);
        }

        if (java9Plus) {
            initJava9PlusCleaner();
        } else {
            initJava8Cleaner();
        }
    }

    private static void initJava9PlusCleaner() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            theUnsafe = theUnsafeField.get(null);

            invokeCleanerMethod = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);

            ByteBuffer testBuffer = ByteBuffer.allocateDirect(1);
            invokeCleanerMethod.invoke(theUnsafe, testBuffer);
            log.debug("ByteBufferClear: Java 9+ Unsafe.invokeCleaner 可用");
        } catch (Throwable e) {
            log.debug("ByteBufferClear: Java 9+ 清理策略不可用, 降级到 Java 8 策略", e);
            theUnsafe = null;
            invokeCleanerMethod = null;
            initJava8Cleaner();
        }
    }

    private static void initJava8Cleaner() {
        try {
            ByteBuffer testBuffer = ByteBuffer.allocateDirect(1);
            cleanerField = testBuffer.getClass().getDeclaredField("cleaner");
            cleanerField.setAccessible(true);

            Object cleaner = cleanerField.get(testBuffer);
            if (cleaner != null) {
                cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.invoke(cleaner);
                log.debug("ByteBufferClear: Java 8 DirectBuffer.cleaner().clean() 可用");
            }
        } catch (Throwable e) {
            log.debug("ByteBufferClear: Java 8 清理策略也不可用, DirectByteBuffer将由GC回收", e);
            cleanerField = null;
            cleanMethod = null;
        }
    }

    public static void cleanDirectBuffer(ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return;
        }
        try {
            if (invokeCleanerMethod != null && theUnsafe != null) {
                invokeCleanerMethod.invoke(theUnsafe, buffer);
            } else if (cleanMethod != null && cleanerField != null) {
                Object cleaner = cleanerField.get(buffer);
                if (cleaner != null) {
                    cleanMethod.invoke(cleaner);
                }
            }
        } catch (Throwable e) {
            log.error("当前无法释放DirectByteBuffer:", e);
        }
    }
}
