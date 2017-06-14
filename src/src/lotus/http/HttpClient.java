package lotus.http;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpClient {
    private URL                 url         = null;
    private boolean             sync        = false;
    private HTTPCallBack        callback    = null;
    private FileStreamHook      streamhook  = null;
    private URLConnection       connection  = null;
    
    private static HttpClient client = null;
    private static Object     slock  = new Object();
    
    public static HttpClient getInstance(){
        if(client == null){
            synchronized (slock) {
                client = new HttpClient();
            }
        }
        return client;
    }
    
    private HttpClient() {
        
    }
    
    public HttpClient init() throws Exception{
        this.url = new URL("");
        connection = this.url.openConnection();
        if(connection instanceof HttpsURLConnection){
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            conn.setSSLSocketFactory(sc.getSocketFactory());
            conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
        }
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.connect();
        return this;
    }
    
    public HttpClient configureBlocking(boolean sync){
        this.sync = sync;
        if(!sync){
            callback = new HTTPCallBack() {
                
            };
        }
        return this;
    }
    
    public HttpClient setHeader(String key, String value){
        connection.setRequestProperty(key, value);
        return this;
    }
    
    public String getHeader(String key){
        return connection.getRequestProperty(key);
    }
    
    public List<String> getResponseHeader(String key){
        return connection.getRequestProperties().get(key);
    }
    
    /**
     * 如果当前不为异步模式则设置回调无效
     * @param callback 
     * @return
     */
    public HttpClient setCallBack(HTTPCallBack callback){
        if(sync){
            this.callback = callback;
        }
        return this;
    }
    
    public HttpClient setFileStreamHook(FileStreamHook streamhook){
        this.streamhook = streamhook;
        return this;
    }
    
    public abstract class HTTPCallBack{
        public void onError(HttpClient client, Exception e){}
        public void onSuccess(HttpClient client, byte[] data){}
        public void onProcess(HttpClient client, long count, long total){}
    }
    
    public abstract class FileStreamHook{
        
        public byte[] read(File file, int offset, byte[] buf){return null;}
        public int write(File file, int offset, byte[] buf){return 0;}
    }
    
    private static class TrustAnyTrustManager implements X509TrustManager {
         
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
 
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
 
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }
 
    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
    }
}
