package or.lotus.wx.support;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClient {
    public static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    static OkHttpClient client;

    static {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
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
    public static String get(String url, Map<String, String> header, Map<String, String> params, int timeout) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if(params != null && params.size() > 0) {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder builder = new Request.Builder();

        builder.get().url(urlBuilder.build());


        if(header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = client.newCall(builder.build());
        if(timeout > 0) {
            call.timeout().timeout(timeout, TimeUnit.MILLISECONDS);
        } else {
            call.timeout().clearTimeout();
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code: " + response);
            }
            return response.body().string();
        } catch (Exception e) {
            log.error("请求出错:" + url, e);
        }

        return "";
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
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            bodyBuilder.add(entry.getKey(), entry.getValue());
        }
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.post(bodyBuilder.build());

        if(header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = client.newCall(builder.build());
        if(timeout > 0) {
            call.timeout().timeout(timeout, TimeUnit.MILLISECONDS);
        } else {
            call.timeout().clearTimeout();
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code: " + response);
            }
            return response.body().string();
        } catch (Exception e) {
            log.error("请求出错:" + url, e);
        }

        return "";
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

        Request.Builder builder = new Request.Builder();
        builder.post(RequestBody.create(json, MediaType.get("application/json")));
        builder.url(url);
        if(header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = client.newCall(builder.build());
        if(timeout > 0) {
            call.timeout().timeout(timeout, TimeUnit.MILLISECONDS);
        } else {
            call.timeout().clearTimeout();
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code: " + response);
            }
            return response.body().string();
        } catch (Exception e) {
            log.error("请求出错:" + url, e);
        }

        return "";
    }

    public static byte[] postJsonWithBytes(String url, Map<String, String> header, String json, int timeout) {

        Request.Builder builder = new Request.Builder();
        builder.post(RequestBody.create(json, MediaType.get("application/json")));
        builder.url(url);

        if(header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = client.newCall(builder.build());
        if(timeout > 0) {
            call.timeout().timeout(timeout, TimeUnit.MILLISECONDS);
        } else {
            call.timeout().clearTimeout();
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code: " + response);
            }
            return response.body().bytes();
        } catch (Exception e) {
            log.error("请求出错:" + url, e);
        }
        return null;
    }

    public static String uploadFile(String url, Map<String, String> header, Map<String, String> params, Map<String, File> files, int timeout) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        if(params != null && params.size() > 0) {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                requestBodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        if(files != null && files.size() > 0) {
            for(Map.Entry<String, File> entry : files.entrySet()) {
                requestBodyBuilder.addFormDataPart(
                        entry.getKey(),
                        entry.getValue().getName(),
                        RequestBody.create(entry.getValue(), MediaType.get("application/octet-stream"))
                );
            }
        }

        builder.post(requestBodyBuilder.build());

        if(header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = client.newCall(builder.build());
        if(timeout > 0) {
            call.timeout().timeout(timeout, TimeUnit.MILLISECONDS);
        } else {
            call.timeout().clearTimeout();
        }
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code: " + response);
            }
            return response.body().string();
        } catch (Exception e) {
            log.error("请求出错:" + url, e);
        }

        return "";
    }

}
