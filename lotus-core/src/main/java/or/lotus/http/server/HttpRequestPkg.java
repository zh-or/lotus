package or.lotus.http.server;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import or.lotus.http.server.exception.HttpMethodNotSupportException;
import or.lotus.http.server.exception.HttpParamsNotFoundException;
import or.lotus.json.JSONException;
import or.lotus.common.BeanUtils;
import or.lotus.common.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestPkg {
    FullHttpRequest rawRequest;
    HttpServer context;
    ChannelHandlerContext channelCtx;
    QueryStringDecoder qsd;
    Map<String, String> parmMap = new HashMap<>();
    private HashMap<String, Object> attrs =   null;

    //解析文件大小(如果是:minSize则会过滤掉16K以下的文件,这个则不限制文件最小长度)
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE);

    public HttpRequestPkg(ChannelHandlerContext channelCtx, HttpServer context, FullHttpRequest rawRequest) throws IOException {
        this.channelCtx = channelCtx;
        this.rawRequest = rawRequest;
        this.context = context;
        attrs = new HashMap<String, Object>();
        qsd = new QueryStringDecoder(rawRequest.uri());

        if (HttpMethod.GET == rawRequest.method()) {
            // 是GET请求
            qsd.parameters().entrySet().forEach( entry -> {
                // entry.getValue()是一个List, 只取第一个元素
                parmMap.put(entry.getKey(), entry.getValue().get(0));
            });
        } else if (HttpMethod.POST == rawRequest.method() && getHeader(HttpHeaderNames.CONTENT_TYPE) != null && getHeader(HttpHeaderNames.CONTENT_TYPE).indexOf("application/x-www-form-urlencoded") != -1) {
            // 是POST请求
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(rawRequest);
            decoder.offer(rawRequest);

            List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();

            for (InterfaceHttpData params : parmList) {
                Attribute data = (Attribute) params;
                parmMap.put(params.getName(), data.getValue());
            }
        }
    }

    /**
     * 简单参数检查, 验证不通过会抛出异常
     * @param keys
     * @throws HttpParamsNotFoundException
     * @throws JSONException
     */
    public void checkParams(HttpMethod method, String[] keys) throws HttpParamsNotFoundException, HttpMethodNotSupportException, JsonProcessingException{
        HttpMethod fmethod = rawRequest.method();
        if(fmethod != method) {
            throw new HttpMethodNotSupportException("不支持的请求方式");
        }
        if(keys == null || keys.length == 0) {
            return;
        }
        if(method == HttpMethod.GET) {
            for(String k : keys) {
                if(Utils.CheckNull(getParam(k))) {
                    throw new HttpParamsNotFoundException(k + " 不能为空");
                }
            }
        } else if(method == HttpMethod.POST) {
            JsonNode json = getBodyJSONObject();
            for(String k : keys) {
                if(!json.has(k)) {
                    throw new HttpParamsNotFoundException(k + " 不能为空");
                }
            }
        }

    }

    public InetSocketAddress getRemoteInetSocketAddress() {
        return (InetSocketAddress) channelCtx.channel().remoteAddress();
    }

    public String getRemoteIpAddress() {
        InetSocketAddress addr = getRemoteInetSocketAddress();
        if(addr == null) {
            return null;
        }
        return addr.getAddress().toString();

        //return channelCtx.channel().remoteAddress().toString();
    }

    public String getRealIp() {
        return getRealIp("real-ip");
    }

    public String getRealIp(String header) {
        String ip = getHeader(header);
        if(Utils.CheckNull(ip)) {
            ip = getRemoteIpAddress();
        }
        if(!Utils.CheckNull(ip))  {
            ip = ip.replace("/", "");
        }
        return ip;
    }

    public ByteBuf getBodyByteBuf() {
        return rawRequest.content();
    }

    private String bodyString = null;//cache

    public String getBodyString() {
        if(bodyString == null) {
            bodyString = getBodyByteBuf().toString(context.charset);
        }
        return bodyString;
    }

    private ObjectNode json;
    public ObjectNode getBodyJSONObject() throws JsonProcessingException {
        if(json == null) {
            json = BeanUtils.parseObject(getBodyString());
        }
        return json;
    }

    public Object getAttr(String key, Object defval){
        Object val = attrs.get(key);
        if(val == null) return defval;
        return val;
    }
    public Object getAttr(String key){
        return getAttr(key, null);
    }

    public void setAttr(String key, Object val){
        attrs.put(key, val);
    }

    public Object removeAttr(String key){
        return attrs.remove(key);
    }

    public String getPath() {
        return qsd.path();
    }

    public String getParam(String name) {
        return parmMap.get(name);
    }

    public int getIntParam(String name) {
        return getIntParam(name, 0);
    }

    public int getIntParam(String name, int def) {
        try {
            return Integer.valueOf(getParam(name));
        } catch(Exception e) {
            //e.printStackTrace();
        }
        return def;
    }

    /**
     * index 从1开始, 顺序为从右到左
     */
    public String getPathParamByIndex(int index) {
        String path = getPath();
        if(index < 1 || Utils.CheckNull(path)) {
            return null;
        }

        String[] pars = path.split("/");
        int len = pars.length;
        if(len > 0 && len > index) {
            return pars[len - index];
        }
        return null;
    }

    /**
     * index 从1开始, 顺序为从右到左
     */
    public int getPathParamByIndexTryInt(int index, int def) {
        try {
            String str = getPathParamByIndex(index);
            if(!Utils.CheckNull(str)) {
                return Integer.valueOf(str);
            }
        } catch (Exception e) { }
        return def;
    }

    public FullHttpRequest raw() {
        return rawRequest;
    }

    public String getHeader(String name) {
        return rawRequest.headers().get(name);
    }

    public String getHeader(CharSequence name) {
        return rawRequest.headers().get(name);
    }

    private int useCount = 0;
    public FullHttpRequest retain() {
        rawRequest = rawRequest.retain();
        useCount ++;
        return rawRequest;
    }

    public void release() {
        if(useCount > 0) {
            useCount --;
            rawRequest.release();
        }
    }

    public boolean isMultipart() {
        return HttpPostRequestDecoder.isMultipart(rawRequest);
    }

   public HttpPostRequestDecoder getPostRequestDecoder() {
        if(isMultipart()) {
            return new HttpPostRequestDecoder(factory, rawRequest);
        }
        return null;
    }

    private HashMap<String, Cookie> cookies = null;

    public HashMap<String, Cookie> getCookies() {
        if(cookies == null) {
            cookies = new HashMap<>();
            String cookieStr = getHeader("Cookie");
            if(!Utils.CheckNull(cookieStr)) {
                List<Cookie> list = ServerCookieDecoder.LAX.decodeAll(cookieStr);
                for (Cookie cookie : list) {
                    cookies.put(cookie.name(), cookie);
                }
            }
        }
        return cookies;
    }

    public Cookie getCookie(String name) {
        getCookies();
        return cookies.get(name);
    }

    public String getCookieValue(String name) {
        Cookie cookie = getCookie(name);
        if(cookie != null) {
            return cookie.value();
        }
        return null;
    }
}
