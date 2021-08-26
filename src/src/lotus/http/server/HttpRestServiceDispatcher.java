package lotus.http.server;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import lotus.http.server.support.HttpMethod;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.http.server.support.HttpServicePath;


public class HttpRestServiceDispatcher extends HttpHandler{
    private ConcurrentHashMap<String, HttpBaseService> services;
    private String baseFilePath;
    
    public HttpRestServiceDispatcher() {
        services = new ConcurrentHashMap<String, HttpBaseService>();
        baseFilePath = "./web";
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
            
        }
        services.put(base, service);
    }
    
    @Override
    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) throws Exception {
        String path = request.getPath();
        try {
            Enumeration<String> keys = services.keys();
            String key;
            while((key = keys.nextElement()) != null) {
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
            
        } catch(Exception e) {
            //exception(e, request, response);
        }
        defFileRequest(baseFilePath, request, response);
    }
}
