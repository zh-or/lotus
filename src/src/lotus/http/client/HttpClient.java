package lotus.http.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lotus.http.Cookie;
import lotus.utils.Utils;

public class HttpClient implements Closeable{
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
    
    private Socket          socket              = null;
    private int             connectionTimeout   = 0;
    private int             readTimeout         = 0;
    private URI             link                = null;
    private Proxy           proxy               = null;
    private SSLContext      sslCtx              = null;
    private Charset         charset             = Charset.forName("utf-8");
    private InputStream     in                  = null;
    private OutputStream    out                 = null;
    private HashMap<String, String> header      = null;
    private HashMap<String, String> resHeader   = null;
    private HashMap<String, Cookie> cookies     = null;
    private ByteArrayOutputStream   body        = null;
    private boolean enableFuckingSocketTimeout = false;
    
    
    private HttpClient(URI link, Proxy proxy) {
        header = new HashMap<>();
        resHeader = new HashMap<>();
        cookies = new HashMap<>();
        body = new ByteArrayOutputStream(4096);
        this.link = link;
        this.proxy = proxy;
        
        buildDefaultHeader();
    }
    
    private void buildDefaultHeader() {
        header.put("Accept", "*/*");
        header.put("Accept-Encoding", "gzip, deflate");
        header.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        header.put("Connection", "keep-alive");
        header.put("Content-Type", "text/plain");
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
        header.put("Host", link.getHost());
        String port = "";

        int tPort = link.getPort();
        if (tPort > 0) {
            port = ":" + String.valueOf(tPort);
        }

        header.put("Host", link.getHost() + port);
    }

    public synchronized HttpClient setRequestHeader(String name, String val) {
        header.put(name, val);
        return this;
    }

    public String getResponseHeader(String name) {
        return resHeader.get(name);
    }

    public Cookie getCookie(String name) {
        return cookies.get(name);
    }

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }


    public HttpClient setTimeout(int timeout) {
        connectionTimeout = timeout;
        readTimeout = timeout;
        return this;
    }
    
    /**
     * 创建socket并连接服务器
     * @return
     * @throws Exception
     */
    public HttpClient connection() throws Exception {
        if(socket != null && socket.isConnected()) {
            return this;
        }
        String scheme = link.getScheme();
        int port = link.getPort();
        String host = link.getHost();
        if("http".equals(scheme)) {
            port = port == -1 ? 80 : port;
            if(proxy != null) {
                socket = new Socket(proxy);
            }else {
                socket = new Socket();
            }
            
        } else if("https".equals(scheme)) {
            port = port == -1 ? 443 : port;
            sslCtx = SSLContext.getInstance("TLSv1.2");
            sslCtx.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
            if(proxy != null) {
                Socket tSock = new Socket(proxy);
                socket = sslCtx.getSocketFactory().createSocket(tSock, host, port, true);
            }else {
                socket = sslCtx.getSocketFactory().createSocket();
            }
        } else {
            throw new Exception("不支持的scheme: " + scheme);
        }
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.connect(new InetSocketAddress(link.getHost(), port), connectionTimeout);
        
        in = socket.getInputStream();
        out = socket.getOutputStream();
        
        return this;
    }
    
    /**
     * 极端情况下可能会出现设置了超时时间也会卡死的情况, 在这种情况下只有另开一个线程处理超时
     */
    public void setEnableFuckingSocketTimeout() {
        enableFuckingSocketTimeout = true;
    }

    private void fuckTimeout(CountDownLatch obj, int timeout) {
        if(!enableFuckingSocketTimeout) {
            return;
        }
        new Thread(() -> {
            synchronized(obj) {
                try {
                    obj.await(timeout, TimeUnit.MILLISECONDS);
                } catch(Exception e) {}
                if(obj.getCount() > 0) {
                    close();
                }
            }
        }, "fuck socket timeout thread -" + obj.hashCode()).start();

    }
    
    public byte[] sendRequest(Method m, String contentType, byte[] bodys) throws Exception {
        body.reset();
        CountDownLatch countdown = new CountDownLatch(2);
        fuckTimeout(countdown, readTimeout + connectionTimeout);
        connection();
        countdown.countDown();
        socket.setSoTimeout(readTimeout);
        //发送请求
        String query = link.getRawQuery();
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
        
        write(path);
        Iterator<Entry<String, String>> it = header.entrySet().iterator();
        while(it.hasNext()) {
            Entry<String, String> kv = it.next();
            write(kv.getKey() + ": " + kv.getValue() + CRLF);
        }
        
        if(!Utils.CheckNull(contentType)) {
            write("Content-Type: " + contentType + CRLF);
        }
        
        if(bodys != null && bodys.length > 0) {
            write("Content-Length: " + bodys.length + CRLF + CRLF);
            write(bodys);
        } else {
            write(CRLF);
        }
        out.flush();
        //读取返回
        resHeader.clear();
        do {
            String line = readLine();
            if(Utils.CheckNull(line)) {
                break;
            }
            String[] kv = line.split(": ");
            if(kv.length == 2) {
                if("set-cookie".equalsIgnoreCase(kv[0])) {
                    Cookie cookie = Cookie.parseFormString(kv[1]);
                    cookies.put(cookie.key, cookie);
                } else {
                    resHeader.put(kv[0].toLowerCase(), kv[1]);
                }
            }
            
        } while(true);
        
        //是否压缩等读取返回数据
        //https://github.dev/germania/httpclient
        String transferEncoding = resHeader.get("transfer-encoding");
        if(!Utils.CheckNull(transferEncoding)) {
            transferEncoding = transferEncoding.trim();
            if("chunked".equalsIgnoreCase(transferEncoding)) {
                int hex;
                String strHex;
                do {
                    strHex = readLine().trim();
                    hex = Integer.parseInt(strHex, 16);
                    readToCache(hex);
                    if(hex > 0) {
                        readLine();
                    }
                } while(hex > 0);
            } else {
                throw new IOException("不支持的 Transfer-Encoding:" + transferEncoding);
            }
        } else {
            //https://blog.csdn.net/itcwg/article/details/112805584
            String contentLen = resHeader.get("content-length");
            int len = -1;
            if(!Utils.CheckNull(contentLen)) {
                len = Integer.valueOf(contentLen);
            }
            //System.out.println("content-len:" + len);
            if(len > 0) {
                readToCache(len);
            }
        }
        countdown.countDown();
        //System.out.println("body len:" + body.size());
        return getBody();
    }
    
    public byte[] getBody() throws IOException {
        byte[] bodyData = body.toByteArray();
        String contentEncoding = resHeader.get("content-encoding");
        InputStream zipIn = null;
        try {
            
            if(!Utils.CheckNull(contentEncoding) && contentEncoding.indexOf("gzip") != -1) {
                zipIn = new GZIPInputStream(new ByteArrayInputStream(bodyData));
            } else if(!Utils.CheckNull(contentEncoding) && contentEncoding.indexOf("deflate") != -1) {
                zipIn = new ZipInputStream(new ByteArrayInputStream(bodyData));
            } else {
                return bodyData;
            }
            ByteArrayOutputStream decodeBody = new ByteArrayOutputStream();
            int b;
            while((b = zipIn.read()) != -1) {
                decodeBody.write(b);
            }
            return decodeBody.toByteArray();
        } finally {
            if(zipIn != null) {
                zipIn.close();
            }
        }
    }
    
    private String readLine() throws IOException {
        StringBuffer sb = new StringBuffer(64);
        char before = 0;
        do {
            char b = (char) (in.read() & 0xff);
            if(b == '\n' && before == '\r') {
                break;
            }
            before = b;
            if(b == '\r') {
                continue;
            }
            sb.append(b);
            
        } while(true);
        
        return sb.toString();
    }
    
    private void readToCache(int len) throws IOException {
        int t = -1;
        for(int i = 0; i < len; i++) {
            t = in.read();
            if(t < 0) {
                return;
            }
            body.write(t);
        }
    }
    
    private void write(String string) throws IOException {
        write(string.getBytes(charset));
    }
    
    private void write(byte[] bs) throws IOException {
        out.write(bs);
    }
    
    public static HttpClient create(String url) throws URISyntaxException {
        return create(url, null);
    }

    public static HttpClient create(String url, Proxy proxy) throws URISyntaxException {
        HttpClient client = new HttpClient(new URI(url), proxy);
        return client;
    }
    
    public static byte[] get(String url, int timeout) throws Exception {
        
        HttpClient client = create(url);
        client.setTimeout(timeout);
        return client.sendRequest(Method.GET, "", null);
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
