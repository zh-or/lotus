package or.lotus.core.http.restful.support;

/**
 * 当有依赖层级问题时, 可通过实现此方法在其他地方返回依赖对象
 * */
public interface ExecFun<T> {
    public T run(Object ...params);
}
