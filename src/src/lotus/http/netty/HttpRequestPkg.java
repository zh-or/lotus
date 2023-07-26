package lotus.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestPkg {
    FullHttpRequest rawRequest;
    HttpServer context;
    ChannelHandlerContext channelCtx;
    QueryStringDecoder qsd;
    Map<String, String> parmMap = new HashMap<>();

    public HttpRequestPkg(ChannelHandlerContext channelCtx, HttpServer context, FullHttpRequest rawRequest) throws IOException {
        this.channelCtx = channelCtx;
        this.rawRequest = rawRequest;
        this.context = context;
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

    public String getPath() {
        return qsd.path();
    }

    public String getParam(String name) {
        return parmMap.get(name);
    }

    public int getIntParam(String name) {
        try {
            return Integer.valueOf(getParam(name));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
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

    public FullHttpRequest retain() {
        rawRequest = rawRequest.retain();
        return rawRequest;
    }
}
