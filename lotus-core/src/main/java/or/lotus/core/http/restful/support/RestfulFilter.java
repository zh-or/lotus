package or.lotus.core.http.restful.support;

import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;

public abstract class RestfulFilter {
    /**调用 controller 之前, 返回 false 表示未处理, 返回 true 表示已处理*/
    public abstract boolean beforeRequest(RestfulRequest request, RestfulResponse response);

    /**调用 controller 之后, 返回 false 表示未处理, 返回 true 表示已处理*/
    public abstract boolean afterRequest(RestfulRequest request, RestfulResponse response);

    /**调用 file 之后, 返回 false 表示未处理, 返回 true 表示已处理 */
    public abstract boolean afterFileRequest(RestfulRequest request, RestfulResponse response);

    /** controller 或其他异常, 返回 false 表示未处理, 返回 true 表示已处理*/
    public abstract boolean exception(Throwable e, RestfulRequest request, RestfulResponse response);
}
