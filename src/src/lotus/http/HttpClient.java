package lotus.http;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpClient {
    private URL url                         = null;
    private boolean sync                    = false;
    private FileProcessCallBack callback    = null;
    private URLConnection       connection  = null;
    
    public HttpClient(String url) throws Exception {
        this.url = new URL(url);
        connection = this.url.openConnection();
        if(connection instanceof HttpsURLConnection){
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            conn.setSSLSocketFactory(sc.getSocketFactory());
            conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
        }else{
            
        }
        connection.setDoInput(true);
        connection.setUseCaches(false);
        
    }
    
    public HttpClient init() throws IOException{
        connection.connect();
        return this;
    }
    
    public HttpClient configureBlocking(boolean sync){
        this.sync = sync;
        return this;
    }
    
    public HttpClient setHeader(String key, String value){
        /*connection.setRequestProperty("Charset", "UTF-8");
        connection.setRequestProperty("Cookie", cookie);*/
        connection.setRequestProperty(key, value);
        return this;
    }
    
    public List<String> getResponseHeader(String key){
        return connection.getRequestProperties().get(key);
    }
    
    public HttpClient setProcessCallBack(FileProcessCallBack callback){
        this.callback = callback;
        return this;
    }
    
    public String getHeader(String key){
        
        return "";
    }
    
    public boolean post(HashMap<String, String> pars){
        
        return false;
    }
    
    public boolean get(){
        
        return false;
    }
    
    public boolean download(File saveto){
        return download(saveto, true);
    }
    
    public boolean download(File saveto, boolean delete){
        
        return false;
    }
    
    
    public boolean upload(String name, File file){
        return upload(new String[]{name}, new File[]{file});
    }
    
    public boolean upload(String[] names,File[] files){
        
        return false;
    }

    public abstract class FileProcessCallBack{
        public void onProcess(File f, long count, long total){}
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
