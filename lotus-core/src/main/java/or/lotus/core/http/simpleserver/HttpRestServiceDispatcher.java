package or.lotus.core.http.simpleserver;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import or.lotus.core.http.simpleserver.support.HttpFileFilter;
import or.lotus.core.http.simpleserver.support.HttpMethod;
import or.lotus.core.http.simpleserver.support.HttpRequest;
import or.lotus.core.http.simpleserver.support.HttpResponse;
import or.lotus.core.http.simpleserver.support.HttpRestErrorHandler;
import or.lotus.core.http.simpleserver.support.HttpRestServiceFilter;
import or.lotus.core.http.simpleserver.support.HttpServicePath;


public class HttpRestServiceDispatcher extends HttpHandler{

    private ConcurrentHashMap<String, HttpBaseService> services;
    private String baseFilePath;
    private ArrayList<HttpRestServiceFilter> filters;
    private HttpFileFilter fileFilter = null;
    private HttpRestErrorHandler errorHandler = null;

    public HttpRestServiceDispatcher() {
        services = new ConcurrentHashMap<String, HttpBaseService>();
        filters = new ArrayList<HttpRestServiceFilter>();
        baseFilePath = "./";
    }

    public synchronized void setErrorHandler(HttpRestErrorHandler handler) {
        errorHandler = handler;
    }

    public synchronized void addServiceFilter(HttpRestServiceFilter filter) {
        filters.add(filter);
    }

    public synchronized void removeServiceFilter(HttpRestServiceFilter filter) {
        filters.remove(filter);
    }

    public void setFileFilter(HttpFileFilter filter) {
        this.fileFilter = filter;
    }

    @Override
    public boolean requestFile(String path, HttpRequest request, HttpResponse response) throws Exception {
        if(fileFilter != null && fileFilter.filter(path, request, response)) {
            return true;
        }
        return false;
    }

    /**
     * 设置静态文件目录
     * @param path
     */
    public void setBaseFilePath(String path) {
        baseFilePath = path;
    }

    public void addService(HttpBaseService service) {
        String base = "/";
        try{
            service.__setDispathcher(this);
            HttpServicePath path = service.getClass().getAnnotation(HttpServicePath.class);
            if(path != null) {
                base = path.path();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        services.put(base, service);
    }

    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) throws Exception {
        for(HttpRestServiceFilter filter : filters) {
            if(filter.filter(request, response)) {
                return;
            }
        }

        String path = request.getPath();
        try {
            Enumeration<String> keys = services.keys();
            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                if(path.startsWith(key)) {
                    HttpBaseService service = services.get(key);
                    if(service != null) {
                        if(service.__dispatch(path.replace(key, ""), request, response)) {
                            //如果已找到方法则直接返回, 否则等循环完后检查是否未静态文件请求
                            return;
                        }
                    }
                }
            }

        } catch(Throwable e) {
            exception(e, request, response);
        }
        defFileRequest(baseFilePath, request, response);
    }

    @Override
    public void exception(Throwable e, HttpRequest request, HttpResponse response) {

        if(errorHandler != null) {
            errorHandler.exception(e, request, response);
            return;
        }
        e.printStackTrace();
    }
}
