package or.lotus.http;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;


public class HttpClient5 {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient5.class);

    private static CloseableHttpClient httpClient;

    private static final CookieStore cookieStore;

    private static final BasicCredentialsProvider basicCredentialsProvider;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final int DEFAULT_MAX_TOTAL = 200;

    private static final int DEFAULT_MAX_PER_ROUTE = 200;

    static {
        // 注册访问协议相关的 Socket 工厂
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        // Http 连接池
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                new PoolingHttpClientConnectionManager(registry);
        poolingHttpClientConnectionManager.setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(30, TimeUnit.SECONDS)
                // tcpNodelay不开启, 此client几乎不用于频繁交互且数据量小的情况
                .setTcpNoDelay(true)
                .build()
        );
        poolingHttpClientConnectionManager.setMaxTotal(DEFAULT_MAX_TOTAL);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        // 在从连接池获取连接时，连接不活跃多长时间后需要进行一次验证
        poolingHttpClientConnectionManager.setValidateAfterInactivity(TimeValue.ofSeconds(5));

        // Http 默认请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000, TimeUnit.MILLISECONDS)
                .setResponseTimeout(5000, TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(5000, TimeUnit.MILLISECONDS)
                .build();

        // 设置 Cookie
        cookieStore = new BasicCookieStore();
        // 设置 Basic Auth 对象
        basicCredentialsProvider = new BasicCredentialsProvider();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                LOGGER.error("HttpClient关闭异常", e);
            }
        }));

        httpClient = HttpClients.custom()
                // 设置 Cookie
                .setDefaultCookieStore(cookieStore)
                // 设置 Basic Auth
                .setDefaultCredentialsProvider(basicCredentialsProvider)
                // 设置 HttpClient 请求参数
                .setDefaultRequestConfig(requestConfig)
                // 设置连接池
                .setConnectionManager(poolingHttpClientConnectionManager)
                // 设置定时清理连接池中过期的连接, 设置这个一定要保证httpclient是单例或者数量一定的!!!
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMinutes(3))
                .build();
    }

    private HttpClient5() { }


    public static CloseableHttpClient getHttpclient() {
        return httpClient;
    }


    private static CookieStore getCookieStore() {
        return cookieStore;
    }

    private static BasicCredentialsProvider getBasicCredentialsProvider() {
        return basicCredentialsProvider;
    }

    public static String get(String url) {
        return get(url, 0);
    }

    public static String get(String url, int timeout) {
        return getWithParams(url, null, timeout);
    }

    /**
     * get请求带参数集合
     * @param url 基础url(没带参数), 如果想自己拼接带参数的url, params需要传null或者空
     * @param params 按照key=value拼接到url上
     * @param timeout 超时时间(毫秒)
     * @return
     */
    public static String getWithParams(String url, Map<String, String> params, int timeout) {
        return get(url, null, params, timeout);
    }

    /**
     * get请求带header
     * @param url
     * @param header header, 若无传null或空集
     * @param timeout 超时时间(毫秒)
     * @return
     */
    public static String getWithHeader(String url, Map<String, String> header, int timeout) {
        return get(url, header, null, timeout);
    }

    /**
     * get请求
     * @param url 基础url(没带参数), 如果想自己拼接带参数的url, params需要传null或者空
     * @param header header, 若无传null或空集
     * @param params 传递参数, 将按照key=value拼接到url上, 若无传null或者空集
     * @param timeout 超时时间(毫秒)
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> header, Map<String, String> params, int timeout){
        return send(url, HttpGet.METHOD_NAME, null, header, params, timeout);
    }

    /**
     * post 表单请求
     * @param url
     * @param params 表单参数, 若无传null或空集
     * @return 响应内容
     */
    public static String postForm(String url, Map<String, String> params) {
        return postForm(url, params, 0);
    }

    /**
     * post 表单请求
     * @param url
     * @param params 表单参数, 若无传null或空集
     * @param timeout 超时时间(毫秒)
     * @return 响应内容
     */
    public static String postForm(String url, Map<String, String> params, int timeout) {
        return postForm(url, null, params, timeout);
    }

    /**
     * post 表单请求
     * @param url
     * @param header header, 若无传null或空集
     * @param params 表单参数, 若无传null或空集
     * @param timeout 超时时间(毫秒)
     * @return 响应内容
     */
    public static String postForm(String url, Map<String, String> header, Map<String, String> params, int timeout) {
        return send(url, HttpPost.METHOD_NAME, ContentType.APPLICATION_FORM_URLENCODED, header, params, timeout);
    }

    /**
     * post json请求
     * @param url
     * @param json json字符串, 若无传null或者空字符串
     * @return 响应内容
     */
    public static String postJson(String url, String json) {
        return postJson(url, json, 0);
    }

    /**
     * post json请求
     * @param url
     * @param json json字符串, 若无传null或者空字符串
     * @param timeout 超时时间(毫秒)
     * @return 响应内容
     */
    public static String postJson(String url, String json, int timeout) {
        return postJson(url, null, json, timeout);
    }

    public static byte[] postJsonWithBytes(String url, String json, int timeout) {
        return postJsonWithBytes(url, null, json, timeout);
    }

    /**
     * post json请求
     * @param url
     * @param header header, 若无传null或空集
     * @param json json字符串, 若无传null或者空字符串
     * @param timeout 超时时间(毫秒)
     * @return 响应内容
     */
    public static String postJson(String url, Map<String, String> header, String json, int timeout) {
        return send(url, HttpPost.METHOD_NAME, ContentType.APPLICATION_JSON, header, json, timeout);
    }

    public static byte[] postJsonWithBytes(String url, Map<String, String> header, String json, int timeout) {

        HttpUriRequestBase request = buildRequest(url, HttpPost.METHOD_NAME, json);

        addHeader(request, ContentType.APPLICATION_JSON, header);
        setRequestConfig(request, timeout);
        setContent(request, ContentType.APPLICATION_JSON, json);

        try(CloseableHttpResponse response = httpClient.execute(request)){
            //LOGGER.info("httpclient response code: {} - {}", response.getCode(), response.getReasonPhrase());

            // 注意这里的response长度不能超过Integer的最大值(可以自定义, 这里使用是默认值)
            byte[] responseContent = EntityUtils.toByteArray(response.getEntity());
            // 确保完全消费
            EntityUtils.consume(response.getEntity());
            return responseContent;
        } catch (Exception e) {
            // todo 自定义异常
            throw new RuntimeException(e);
        }
    }

    private static String send(String url, String methodName, ContentType contentType, Map<String, String> header, Object content, int timeout) {
        HttpUriRequestBase request = buildRequest(url, methodName, content);

        addHeader(request, contentType, header);
        setRequestConfig(request, timeout);
        setContent(request, contentType, content);

        return getResponse(request);
    }

    private static String getResponse(HttpUriRequestBase request) {
        // 发起请求
        try(CloseableHttpResponse response = httpClient.execute(request)){
            //LOGGER.info("httpclient response code: {} - {}", response.getCode(), response.getReasonPhrase());

            // 注意这里的response长度不能超过Integer的最大值(可以自定义, 这里使用是默认值)
            String responseContent = EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
            // 确保完全消费
            EntityUtils.consume(response.getEntity());
            return responseContent;
        } catch (Exception e) {
            // todo 自定义异常
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建请求
     * @param url
     * @param methodName
     * @param content get请求为params, 按照?key1=value1&key2=value2拼接, post请求直接设置url
     * @return
     */
    private static HttpUriRequestBase buildRequest(String url, String methodName, Object content) {
        if (HttpGet.METHOD_NAME.equals(methodName)) {
            Map<String, String> params = (Map<String, String>)content;
            if (params == null || params.isEmpty()){
                return new HttpGet(url);
            }else {
                StringJoiner urlJoiner = new StringJoiner("&");

                for (Map.Entry<String, String> param : params.entrySet()) {
                    urlJoiner.add(param.getKey() + "=" + param.getValue());
                }

                return new HttpGet(url + "?" + urlJoiner);
            }
        }

        if (HttpPost.METHOD_NAME.equals(methodName)) {
            return new HttpPost(url);
        }

        return null;
    }

    private static void addHeader(HttpUriRequestBase request, ContentType contentType, Map<String, String> header) {
        // post请求设置contentType请求头
        if (request instanceof HttpPost) {
            request.addHeader("Content-Type", contentType.getMimeType());
        }

        if (header == null || header.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> headerEntry : header.entrySet()) {
            request.addHeader(headerEntry.getKey(), headerEntry.getValue());
        }
    }

    private static void setRequestConfig(HttpUriRequestBase request, int timeout) {
        // timeout为0走默认配置
        if (timeout == 0) {
            return;
        }
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                .setResponseTimeout(timeout, TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(timeout, TimeUnit.MILLISECONDS).build();

        request.setConfig(requestConfig);
    }

    private static void setContent(HttpUriRequestBase request, ContentType contentType, Object content) {
        if (contentType == null || content == null) {
            return;
        }

        if (ContentType.APPLICATION_FORM_URLENCODED == contentType) {
            List<NameValuePair> paramList = new ArrayList<>();
            Map<String, String> contentMap = (Map<String, String>) content;
            if (contentMap.isEmpty()) {
                return;
            }
            for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            request.setEntity(new UrlEncodedFormEntity(paramList, DEFAULT_CHARSET));
        }else if (ContentType.APPLICATION_JSON == contentType) {
            if (((String)content).isEmpty()) {
                return;
            }
            request.setEntity(new StringEntity((String) content, DEFAULT_CHARSET));
        }
    }

}
