package lotus.http.server;


public abstract class HttpHandler {

    public void service(HttpMethod mothed, HttpRequest request, HttpResponse response){
        switch (mothed) {
            case GET:
                this.get(request, response);
                break;
            case POST:
                this.post(request, response);
                break;
            case CONNECT:
                this.connect(request, response);
                break;
            case DELETE:
                this.delete(request, response);
                break;
            case HEAD:
                this.head(request, response);
                break;
            case OPTIONS:
                this.options(request, response);
                break;
            case PUT:
                this.put(request, response);
                break;
            case TRACE:
                this.trace(request, response);
                break;
        }
    }
    
    public void get(HttpRequest request, HttpResponse response){}
    public void post(HttpRequest request, HttpResponse response){}
    public void connect(HttpRequest request, HttpResponse response){}
    public void delete(HttpRequest request, HttpResponse response){}
    public void head(HttpRequest request, HttpResponse response){}
    public void options(HttpRequest request, HttpResponse response){}
    public void put(HttpRequest request, HttpResponse response){}
    public void trace(HttpRequest request, HttpResponse response){}
}
