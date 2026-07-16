package or.lotus.core.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * 哈希时间轮定时器, 参考Netty实现优化, 用于高效处理大量session的空闲检测
 * <p>
 * 核心优化:
 * <ul>
 *   <li>双队列架构: I/O线程仅做无锁入队, 所有bucket操作由worker单线程完成, 消除锁竞争</li>
 *   <li>System.nanoTime(): 单调时钟, 不受系统时间调整影响</li>
 *   <li>直接链表遍历: 无Iterator开销</li>
 *   <li>空闲阻塞: 无活跃连接时worker线程park, 零CPU开销</li>
 * </ul>
 */
public class HashedWheelTimer {

    static final Logger log = LoggerFactory.getLogger(HashedWheelTimer.class);

    private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final int WORKER_STATE_INIT = 0;
    private static final int WORKER_STATE_STARTED = 1;
    private static final int WORKER_STATE_SHUTDOWN = 2;

    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    private final long tickDuration;
    private final int wheelSize;
    private final int wheelMask;
    private final HashedWheelBucket[] wheel;

    private final Worker worker = new Worker();
    private final Thread workerThread;
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    private volatile long startTime;

    @SuppressWarnings("unused")
    private volatile int workerState = WORKER_STATE_INIT;

    /** I/O线程: 新的timeout入队(无锁) */
    private final ConcurrentLinkedQueue<HashedWheelTimeout> pendingTimeouts = new ConcurrentLinkedQueue<>();

    /** I/O线程: 取消的timeout入队(无锁) */
    private final ConcurrentLinkedQueue<HashedWheelTimeout> cancelledTimeouts = new ConcurrentLinkedQueue<>();

    /** 当前活跃的timeout数量 */
    private final AtomicInteger timeoutCount = new AtomicInteger(0);

    public HashedWheelTimer() {
        this(200, 512);
    }

    public HashedWheelTimer(long tickDurationMs, int wheelSize) {
        if (tickDurationMs <= 0) {
            throw new IllegalArgumentException("tickDuration must be positive");
        }
        if (wheelSize <= 0 || (wheelSize & (wheelSize - 1)) != 0) {
            throw new IllegalArgumentException("wheelSize must be a power of 2");
        }
        this.tickDuration = MILLISECOND_NANOS * tickDurationMs;
        this.wheelSize = wheelSize;
        this.wheelMask = wheelSize - 1;
        this.wheel = createWheel(wheelSize);
        this.workerThread = new Thread(worker, "HashedWheelTimer-Worker");
        this.workerThread.setDaemon(true);
    }

    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < ticksPerWheel; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    public void start() {
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
            return;
        }
        workerThread.start();
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
            }
        }
    }

    public void stop() {
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            if (WORKER_STATE_UPDATER.get(this) == WORKER_STATE_SHUTDOWN) {
                return;
            }
            WORKER_STATE_UPDATER.set(this, WORKER_STATE_SHUTDOWN);
            return;
        }

        LockSupport.unpark(workerThread);

        boolean interrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100);
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < wheelSize; i++) {
            wheel[i].clear();
        }
        pendingTimeouts.clear();
        cancelledTimeouts.clear();
    }

    public void addTimeout(Session session, long idleTimeMs) {
        if (session == null || idleTimeMs <= 0) {
            return;
        }

        // 取消旧timeout
        HashedWheelTimeout old = session.wheelTimeout;
        if (old != null) {
            session.wheelTimeout = null;
            old.cancelled = true;
            cancelledTimeouts.add(old);
        }

        long deadline = System.nanoTime() + MILLISECOND_NANOS * idleTimeMs - startTime;
        if (idleTimeMs > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }

        HashedWheelTimeout timeout = new HashedWheelTimeout(session, deadline, idleTimeMs);
        session.wheelTimeout = timeout;
        pendingTimeouts.add(timeout);
        timeoutCount.incrementAndGet();

        LockSupport.unpark(workerThread);
    }

    public void resetTimeout(Session session, long idleTimeMs) {
        if (session == null || idleTimeMs <= 0) {
            return;
        }
        addTimeout(session, idleTimeMs);
    }

    public void removeTimeout(Session session) {
        if (session == null) {
            return;
        }
        HashedWheelTimeout timeout = session.wheelTimeout;
        if (timeout != null) {
            session.wheelTimeout = null;
            timeout.cancelled = true;
            cancelledTimeouts.add(timeout);
            LockSupport.unpark(workerThread);
        }
    }

    public int getTimeoutCount() {
        return timeoutCount.get();
    }

    public boolean isRunning() {
        return workerState == WORKER_STATE_STARTED;
    }

    public long getTickDuration() {
        return TimeUnit.NANOSECONDS.toMillis(tickDuration);
    }

    public int getWheelSize() {
        return wheelSize;
    }


    private final class Worker implements Runnable {
        private long tick;

        @Override
        public void run() {
            startTime = System.nanoTime();
            if (startTime == 0) {
                startTime = 1;
            }
            startTimeInitialized.countDown();

            while (workerState == WORKER_STATE_STARTED) {
                // 1. 处理取消队列
                processCancelledTasks();
                // 2. 转移pending队列到bucket
                transferTimeoutsToBuckets();

                // 3. 无活跃timeout, park等待唤醒
                if (timeoutCount.get() == 0) {
                    LockSupport.park();
                    if (workerState != WORKER_STATE_STARTED) {
                        break;
                    }
                    continue;
                }

                // 4. 等待下一个tick
                waitForNextTick();
                if (workerState != WORKER_STATE_STARTED) {
                    break;
                }

                // 5. 处理当前bucket
                int idx = (int) (tick & wheelMask);
                wheel[idx].expireTimeouts(tickDuration * (tick + 1), timeoutCount);
                tick++;
            }

            // 清理: 收集未处理的timeout
            for (int i = 0; i < wheelSize; i++) {
                wheel[i].clearTimeouts();
            }
            HashedWheelTimeout timeout;
            while ((timeout = pendingTimeouts.poll()) != null) {
                if (!timeout.cancelled) {
                    timeout.session.wheelTimeout = null;
                }
            }
            while ((timeout = cancelledTimeouts.poll()) != null) {
                // already handled
            }
        }

        private void processCancelledTasks() {
            HashedWheelTimeout timeout;
            while ((timeout = cancelledTimeouts.poll()) != null) {
                if (timeout.bucket != null) {
                    timeout.bucket.remove(timeout);
                }
                timeoutCount.decrementAndGet();
            }
        }

        private void transferTimeoutsToBuckets() {
            for (int i = 0; i < 100000; i++) {
                HashedWheelTimeout timeout = pendingTimeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (timeout.cancelled) {
                    continue;
                }

                long calculated = timeout.deadline / tickDuration;
                timeout.remainingRounds = (calculated - tick) / wheelSize;
                long ticks = Math.max(calculated, tick);
                int idx = (int) (ticks & wheelMask);

                HashedWheelBucket bucket = wheel[idx];
                bucket.addTimeout(timeout);
            }
        }

        private void waitForNextTick() {
            long deadline = tickDuration * (tick + 1);
            for (;;) {
                long currentTime = System.nanoTime() - startTime;
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                if (sleepTimeMs <= 0) {
                    return;
                }

                if (IS_WINDOWS) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                    if (sleepTimeMs == 0) {
                        sleepTimeMs = 1;
                    }
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e) {
                    if (workerState == WORKER_STATE_SHUTDOWN) {
                        return;
                    }
                }
            }
        }
    }


    private static final class HashedWheelBucket {
        private HashedWheelTimeout head;
        private HashedWheelTimeout tail;

        void addTimeout(HashedWheelTimeout timeout) {
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        void remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            HashedWheelTimeout prev = timeout.prev;

            if (prev != null) {
                prev.next = next;
            } else {
                head = next;
            }

            if (next != null) {
                next.prev = prev;
            } else {
                tail = prev;
            }

            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
        }

        void expireTimeouts(long deadline, AtomicInteger timeoutCount) {
            HashedWheelTimeout timeout = head;
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;

                if (timeout.cancelled) {
                    remove(timeout);
                } else if (timeout.remainingRounds <= 0) {
                    timeout.expire(deadline);
                    timeoutCount.decrementAndGet();
                } else {
                    timeout.remainingRounds--;
                }

                timeout = next;
            }
        }

        void clearTimeouts() {
            HashedWheelTimeout timeout = head;
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                timeout.prev = null;
                timeout.next = null;
                timeout.bucket = null;
                timeout.session.wheelTimeout = null;
                timeout = next;
            }
            head = null;
            tail = null;
        }

        void clear() {
            HashedWheelTimeout timeout = head;
            while (timeout != null) {
                timeout.cancelled = true;
                HashedWheelTimeout next = timeout.next;
                timeout.next = null;
                timeout.prev = null;
                timeout.bucket = null;
                timeout = next;
            }
            head = null;
            tail = null;
        }
    }

    static final class HashedWheelTimeout {
        volatile Session session;
        volatile long deadline;
        volatile long idleTimeMs;
        volatile long remainingRounds;
        volatile boolean cancelled = false;
        volatile HashedWheelBucket bucket;

        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        HashedWheelTimeout(Session session, long deadline, long idleTimeMs) {
            this.session = session;
            this.deadline = deadline;
            this.idleTimeMs = idleTimeMs;
        }

        void expire(long deadline) {
            if (bucket != null) {
                bucket.remove(this);
            }

            Session s = session;
            s.wheelTimeout = null;

            if (!s.isClosed()) {
                s.pushEventRunnable(new IoEventRunnable(null, IoEventRunnable.IoEventType.SESSION_IDLE, s, s.context));
            }
        }
    }
}
