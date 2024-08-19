package or.lotus.core.http.server;

import java.util.ArrayList;

public class HttpServiceWrap {
    public String key;
    public ArrayList<HttpBaseService> services;

    public HttpServiceWrap(String key) {
        this.key = key;
        this.services = new ArrayList<>(1);
    }

    public void addService(HttpBaseService service) {
        this.services.add(service);
    }
}
