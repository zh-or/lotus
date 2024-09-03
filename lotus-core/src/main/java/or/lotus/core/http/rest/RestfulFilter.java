package or.lotus.core.http.rest;

public abstract class RestfulFilter {
    /**调用 controller 之前, 返回 null 表示不处理, 否则表示拦截并输出返回值*/
    public abstract Object beforeRequest(RestfulRequest request);

    /**调用 controller 之后, 返回 null 表示不处理, 否则表示拦截并输出返回值*/
    public abstract Object afterRequest(RestfulRequest request, Object response);

    /**调用 file 之后, 返回 null 表示不处理, 否则表示拦截并输出返回值*/
    public abstract Object afterFileRequest(RestfulRequest request, Object response);

    /** controller 或其他异常 */
    public abstract Object exception(Throwable e, RestfulRequest request);
}
