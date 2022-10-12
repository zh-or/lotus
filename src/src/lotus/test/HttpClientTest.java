package lotus.test;

import java.net.URISyntaxException;
import java.nio.charset.Charset;

import lotus.http.client.simple.SimpleHTTPClient;

public class HttpClientTest {
    
    public static void main(String[] args) throws Exception {
        //byte[] res = SimpleHTTPClient.get("http://lushu.fun:35534/api/state", 60 * 1000);
        byte[] res = SimpleHTTPClient.get("https://tool.oschina.net/js/bootstrap/css/bootstrap.min.css", 60 * 1000);
        String str = new String(res, Charset.forName("utf-8"));
        System.out.println("res:" + str);
        
    }
}
