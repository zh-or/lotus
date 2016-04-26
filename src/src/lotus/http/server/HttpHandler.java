package lotus.http.server;


public abstract class HttpHandler {

    public void service(HttpMethod mothed, Request request, Response response){
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
    
    public void get(Request request, Response response){}
    public void post(Request request, Response response){}
    public void connect(Request request, Response response){}
    public void delete(Request request, Response response){}
    public void head(Request request, Response response){}
    public void options(Request request, Response response){}
    public void put(Request request, Response response){}
    public void trace(Request request, Response response){}
}
