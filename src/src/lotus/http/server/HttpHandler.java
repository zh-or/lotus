package lotus.http.server;


public abstract class HttpHandler {
    public static final String MOTHED_POST      =   "POST";
    public static final String MOTHED_GET       =   "GET";
    /*其它的?go die*/
    public void service(String mothed, Request request, Response response){
        switch (mothed) {
            case MOTHED_GET:
                this.get(request, response);
                break;
            case MOTHED_POST:
                this.post(request, response);
                break;
        }
    }
    public void get(Request request, Response response){}
    public void post(Request request, Response response){}
}
