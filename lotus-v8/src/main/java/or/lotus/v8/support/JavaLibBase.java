package or.lotus.v8.support;


import or.lotus.v8.Message;
import or.lotus.v8.V8Context;

public abstract class JavaLibBase {

    /**
     * 在此方法内初始化, 可以向JavaScript注册回调函数
     * @param v8b
     */
    public abstract void onInit(V8Context v8b);

    /**
     * js 或者系统发出quit消息
     */
    public abstract void onQuit();

    /**
     * 销毁
     */
    public abstract void onDestroy();

    public abstract boolean MessageLoop(Message msg);

}
