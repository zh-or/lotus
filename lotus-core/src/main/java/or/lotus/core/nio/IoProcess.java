package or.lotus.core.nio;

import or.lotus.core.nio.tcp.NioTcpServer;
import or.lotus.core.nio.tcp.NioTcpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class IoProcess extends Thread {
    public static final Logger log = LoggerFactory.getLogger(IoProcess.class);

    protected NioContext context = null;

    protected LinkedBlockingQueue<Runnable> pendingTasks = null;

    public IoProcess(NioContext context) {
        this.context = context;
        pendingTasks = new LinkedBlockingQueue<>();
        setName("Lotus Nio IoProcess");
    }

    public void addPendingTask(Runnable run) {
        pendingTasks.add(run);
    }

    @Override
    public void run() {
        while (context.isRunning) {
            try {
                Runnable run;
                while((run = pendingTasks.poll()) != null) {
                    run.run();
                }

                process();
            } catch (Exception e) {
                log.debug("IoProcess 存在未处理的异常:", e);
            }
        }
    }

    public abstract void wakeup();

    /** 实现此方法处理io */
    public abstract void process();

    @Override
    public synchronized void start() {
        if(!context.isRunning) {
            throw new RuntimeException("启动时需先设置 context.isRunning 为 true");
        }
        super.start();
    }

    /** 服务器停止时调用, 调用前需要争 context.isRunning = false,然后等待process方法结束 */
    public void close() {
        interrupt();//中断阻塞
        try {
            if (isAlive()) {
                join(5000); // 等待最多5秒
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
    }

}
