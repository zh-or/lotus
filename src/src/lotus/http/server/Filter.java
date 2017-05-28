package lotus.http.server;

public class Filter {
    public String path;
    public HttpHandler handler;

    public Filter(String path, HttpHandler handler) {
        this.path = path;
        this.handler = handler;
    }
    
}
