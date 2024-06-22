package or.lotus.test;

import or.lotus.queue.delay.DelayQueueCallBack;
import or.lotus.queue.delay.DelayQueueExecutor;
import or.lotus.queue.delay.DelayQueueRetryException;
import or.lotus.queue.delay.DelayTaskExec;
import or.lotus.common.Format;
import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DelayQueueTest {
    static final Logger log = LoggerFactory.getLogger(DelayQueueTest.class);


    public static void main(String[] args) throws Exception {
        DelayQueueExecutor test = new DelayQueueExecutor(new DelayQueueCallBack() {
            @Override
            public void onInit(DelayQueueExecutor context) {
                log.info("初始化调用");
            }

            @Override
            public void onAddTask(DelayQueueExecutor context, String type, long execTime, Object obj) {

                log.info("添加延迟任务成功, 执行时间: {}, obj: {}", Format.formatTime(execTime), obj);
            }

            @Override
            public void onRetryException(DelayQueueExecutor context, String type, long execTime, Object obj) {

                log.error("执行延迟任务出错, {} 自动重试,  type: {}, obj: {}",
                        Format.formatTime(execTime),
                        type,
                        obj);
            }

            @Override
            public void onSuccess(DelayQueueExecutor context, String type, long execTime, Object obj, String result) {
                log.info("任务执行成功调用, result:{}", result);
            }

            @Override
            public void onUnknownException(DelayQueueExecutor context, String type, long execTime, Object obj, Exception e) {

                log.error("执行延迟任务出错,  type: {}, obj: {}, e: {}",
                        type,
                        obj,
                        e);
            }
        });

        test.registerTaskExec("t1", new DelayTaskExec<TestObj>() {
            @Override
            public String exec(DelayQueueExecutor exec, String type, TestObj obj) throws Exception {
                log.info("执行任务, type: {}, obj: {}", type, obj);
                int a = Utils.RandomNum(0, 9);
                if(a > 5) {
                    throw new DelayQueueRetryException("测试十秒后重试", 10);
                }

                if(a <= 1) {
                    throw new Exception("测试未知异常");
                }
                return Utils.RandomNum(4);
            }
        });

        test.addTask("t1", System.currentTimeMillis() + 3000, new TestObj("test obj 3"));
        test.addTask("t1", System.currentTimeMillis() + 5000, new TestObj("test obj 5"));
        test.addTask("t1", System.currentTimeMillis() + 8000, new TestObj("test obj 8"));
    }


    static class TestObj {
        String name;

        public TestObj(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "TestObj{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
