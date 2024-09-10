package or.lotus.core.http.restful;

import or.lotus.core.http.ApiRes;
import or.lotus.core.http.RestfulApiException;
import or.lotus.core.http.restful.RestfulDispatcher;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.support.RestfulDispatchMapper;

import java.io.IOException;

public abstract class RestfulFilter {
    /**调用 controller 之前, 返回 false 表示未处理, 返回 true 表示已处理
     * 需要注意dispatcher可能为空
     * */
    public boolean beforeRequest(RestfulDispatcher dispatcher, RestfulRequest request, RestfulResponse response) {
        return false;
    }

    /**调用 controller 之后, 返回 false 表示未处理, 返回 true 表示已处理*/
    public boolean afterRequest(RestfulRequest request, RestfulResponse response, Object controllerResult) {
        return false;
    }

    /** controller 或其他异常, 返回 false 表示未处理, 返回 true 表示已处理*/
    public boolean exception(Throwable e, RestfulRequest request, RestfulResponse response) {
        if(e instanceof RestfulApiException) {
            try {
                response.clearWrite().write(ApiRes.error(e.getMessage()).toString());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return true;
        }
        return false;
    }
}
