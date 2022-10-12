package lotus.test;

import java.net.URISyntaxException;
import java.nio.charset.Charset;

import lotus.http.client.HttpClient;

public class HttpClientTest {
    
    public static void main(String[] args) throws Exception {
        //byte[] res = SimpleHTTPClient.get("http://lushu.fun:35534/api/state", 60 * 1000);
        byte[] res = HttpClient.get("https://www.hdmi.org/odata/adoptersaffiliates?%24format=json&%24orderby=AdopterOrAffiliate&%24top=100&%24skip=0&%24filter=((AffiliateStatusId%20eq%202%20or%20AdopterStatusId%20eq%201%20or%20AdopterStatusId%20eq%2011%20or%20AdopterStatusId%20eq%2012%20or%20AdopterStatusId%20eq%2020)%20and%20AdopterId%20ne%205097)&%24count=true", 60 * 1000);
        String str = new String(res, Charset.forName("utf-8"));
        System.out.println("res:" + str);
        
    }
}
