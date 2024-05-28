package or.lotus.http.simpleserver.support;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import or.lotus.http.simpleserver.HttpServer;

public class HttpServerX509TrustManager implements X509TrustManager {
    private HttpServer context;

    public HttpServerX509TrustManager(HttpServer context) {
        this.context = context;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {

        return null;
    }

}
