package lotus.http.netty;

public class RequestPathNotFound extends Exception{
    public RequestPathNotFound(String message) {
        super(message);
    }
}
