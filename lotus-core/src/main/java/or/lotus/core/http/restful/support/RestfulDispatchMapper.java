package or.lotus.core.http.restful.support;

import or.lotus.core.http.restful.RestfulDispatcher;

public class RestfulDispatchMapper {
    private RestfulDispatcher getDispatcher;
    private RestfulDispatcher postDispatcher;
    private RestfulDispatcher putDispatcher;
    private RestfulDispatcher deleteDispatcher;
    private RestfulDispatcher requestDispatcher;

    public RestfulDispatchMapper() {
    }

    public RestfulDispatchMapper(RestfulDispatcher dispatcher) {
        setDispatcher(dispatcher);
    }

    public void setDispatcher(RestfulDispatcher dispatcher) {
        if(dispatcher.httpMethod == RestfulHttpMethod.GET)
            getDispatcher = dispatcher;
        else if(dispatcher.httpMethod == RestfulHttpMethod.POST)
            postDispatcher = dispatcher;
        else if(dispatcher.httpMethod == RestfulHttpMethod.PUT)
            putDispatcher = dispatcher;
        else if(dispatcher.httpMethod == RestfulHttpMethod.DELETE)
            deleteDispatcher = dispatcher;
        else if(dispatcher.httpMethod == RestfulHttpMethod.REQUEST)
            requestDispatcher = dispatcher;
        else
            throw new RuntimeException("未知的dispatcher类型");
    }

    /** 当 控制器为 @Request 注解时直接返回该方法 */
    public RestfulDispatcher getDispatcher(RestfulHttpMethod method) {
        if(requestDispatcher != null) {
            return requestDispatcher;
        }
        if(method == RestfulHttpMethod.GET)
            return getDispatcher;
        else if(method == RestfulHttpMethod.POST)
            return postDispatcher;
        else if(method == RestfulHttpMethod.PUT)
            return putDispatcher;
        else if(method == RestfulHttpMethod.DELETE)
            return deleteDispatcher;
        else if(method == RestfulHttpMethod.OPTIONS) {
            if(getDispatcher != null) return getDispatcher;
            if(postDispatcher != null) return postDispatcher;
            if(putDispatcher != null) return putDispatcher;
            if(deleteDispatcher != null) return deleteDispatcher;
        }
       return null;
    }

}
