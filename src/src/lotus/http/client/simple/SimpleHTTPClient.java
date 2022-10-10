package lotus.http.client.simple;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.rmi.CORBA.Util;

import lotus.utils.Utils;

public class SimpleHTTPClient {
    public String CRLF = "\r\n";
    public enum Method {
        GET("GET"),
        POST("POST"),
        DELETE("DELETE"),
        PUT("PUT");
        
        String m;
        Method(String m) {
            this.m = m;
        }
        
        @Override
        public String toString() {
            return m;
        }
    }
    
    private Socket      socket              = null;
    private int         connectionTimeout   = 0;
    private int         readTimeout         = 0;
    private URI         link                = null;
    private Proxy       proxy               = null;
    private SSLContext  sslCtx              = null;
    private Charset     charset             = Charset.forName("utf-8");
    private HashMap<String, String> header  = null;
    
    
    private SimpleHTTPClient(URI link, Proxy proxy) {
        header = new HashMap<>();
        this.link = link;
        this.proxy = proxy;
        buildDefaultHeader();
    }
    
    public SimpleHTTPClient setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
        return this;
    }
    
    public SimpleHTTPClient setReadTimeout(int timeout) {
        readTimeout = timeout;
        return this;
    }
    
    /**
     * 创建socket并连接服务器
     * @return
     * @throws Exception
     */
    public SimpleHTTPClient connection() throws Exception {
        if(socket != null && socket.isConnected()) {
            return this;
        }
        String scheme = link.getScheme();
        int port = link.getPort();
        String host = link.getHost();
        if("http".equals(scheme)) {
            port = port == -1 ? 80 : port;
            if(proxy != null) {
                this.socket = new Socket(proxy);
            }else {
                this.socket = new Socket();
            }
            
        } else if("https".equals(scheme)) {
            port = port == -1 ? 443 : port;
            sslCtx = SSLContext.getInstance("TLSv1.2");
            sslCtx.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
            if(proxy != null) {
                Socket tSock = new Socket(proxy);
                this.socket = sslCtx.getSocketFactory().createSocket(tSock, host, port, true);
            }else {
                this.socket = sslCtx.getSocketFactory().createSocket();
            }
        } else {
            throw new Exception("不支持的scheme: " + scheme);
        }
        socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);
        this.socket.connect(new InetSocketAddress(link.getHost(), port), connectionTimeout);
        
        
        return this;
    }
    
    
    public byte[] sendRequest(Method m, String contentType, byte[] bodys) throws Exception {
        connection();
        socket.setSoTimeout(readTimeout);
        //发送请求
        try(OutputStream out = socket.getOutputStream()) {
            String query = link.getQuery();
            if(Utils.CheckNull(query)) {
                query = "";
            } else {
                query = "?" + query;
            }
            String path = String.format(
                    "%s %s%s HTTP/1.1%s", 
                    m.toString(),
                    link.getPath(),
                    query,
                    CRLF
                    );
            
            write(out, path);
            Iterator<Entry<String, String>> it = header.entrySet().iterator();
            while(it.hasNext()) {
                Entry<String, String> kv = it.next();
                write(out, kv.getKey() + ": " + kv.getValue() + CRLF);
            }
            
            if(!Utils.CheckNull(contentType)) {
                write(out, "Content-Type: " + contentType + CRLF);
            }
            
            if(bodys != null && bodys.length > 0) {
                write(out, "Content-Length: " + bodys.length + CRLF + CRLF);
                write(out, bodys);
            } else {
                write(out, CRLF);
            }
            out.flush();
        }
        //读取返回
        byte[] body = null;
        HashMap<String, String> resHeader = new HashMap<String, String>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            do {
                String line = br.readLine();
                if(Utils.CheckNull(line)) {
                    break;
                }
                String[] kv = line.split(": ");
                if(kv.length == 2) {
                    resHeader.put(kv[0].toLowerCase(), kv[1]);
                }
                
            } while(true);
            
            //是否压缩等读取返回数据
            //https://github.dev/germania/httpclient
            String transferEncoding = resHeader.get("transfer-encoding");
            if(!Utils.CheckNull(transferEncoding)) {
                transferEncoding = transferEncoding.trim();
                if("chunked".equalsIgnoreCase(transferEncoding)) {

                } else {
                    throw new IOException("不支持的 Transfer-Encoding:" + transferEncoding);
                }
            } else {
                String contentEncoding = resHeader.getHeaderField("content-encoding");
	            if(!Utils.CheckNull(contentEncoding) && contentEncoding.indexOf("gzip") != -1) {
	                //is = new GZIPInputStream(is);
	            }
                //https://blog.csdn.net/itcwg/article/details/112805584
                String contentLen = resHeader.get("content-length");
                int len = -1;
                if(!Utils.CheckNull(contentLen)) {
                    len = Integer.valueOf(contentLen);
                }

                if(len > 0) {
                    body = new byte[len];
                }
            }

            
        }
        
        
        return body;
    }
    
    public byte[] readBody() {
        
        return null;
    }
    
    
    private void write(OutputStream out, String string) throws IOException {
        write(out, string.getBytes(charset));
    }
    
    private void write(OutputStream out, byte[] bs) throws IOException {
        out.write(bs);
    }
    
    private void buildDefaultHeader() {
        header.put("Accept", "*/*");
        header.put("Accept-Encoding", "gzip, deflate");
        header.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        header.put("Connection", "keep-alive");
        header.put("Content-Type", "text");
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
        
        String port = "";
        
        int tPort = link.getPort();
        if(tPort > 0) {
            port = ":" + String.valueOf(tPort);
        }
        
        header.put("Host", link.getHost() + port);
    }
    
    public synchronized SimpleHTTPClient setHeader(String name, String val) {
        header.put(name, val);
        return this;
    }
    
    public void close() {
        try {
            if(socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static SimpleHTTPClient create(String url, Proxy proxy) throws URISyntaxException {
        SimpleHTTPClient client = new SimpleHTTPClient(new URI(url), proxy);
        return client;
    }
    
    public static byte[] get(String url, int timeout) throws URISyntaxException {
        
        SimpleHTTPClient client = create(url, null);
        client.connectionTimeout = timeout;
        client.readTimeout = timeout;
        return client.readBody();
    }
    
    
    private static class TrustAnyTrustManager implements X509TrustManager {
        
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            
        }
 
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }
 
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }
}
