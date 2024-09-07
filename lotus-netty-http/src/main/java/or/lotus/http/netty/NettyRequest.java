package or.lotus.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import or.lotus.core.http.restful.RestfulContext;
import or.lotus.core.http.restful.RestfulFormData;
import or.lotus.core.http.restful.RestfulRequest;
import or.lotus.core.http.restful.support.RestfulHttpMethod;
import or.lotus.http.netty.exception.NettyParameterException;


import java.net.SocketAddress;

public class NettyRequest extends RestfulRequest {

    public ChannelHandlerContext channel;
    FullHttpRequest msg;
    QueryStringDecoder qsd;
    public NettyRequest(RestfulContext context, ChannelHandlerContext channel, FullHttpRequest msg) {
        super(context);
        this.channel = channel;
        this.msg = msg;
        qsd = new QueryStringDecoder(msg.uri());
    }

    public void checkParameter(String name, String exceptionMsg) {
        if(getParameter(name) == null) {
            throw new NettyParameterException(exceptionMsg);
        }
    }

    FullHttpRequest rawRequest() {
        return msg;
    }

    private int useCount = 0;
    public FullHttpRequest retain() {
        msg = msg.retain();
        useCount ++;
        return msg;
    }

    /**只释放自己加的引用*/
    public void release() {
        if(useCount > 0) {
            useCount --;
            msg.release();
        }
    }
    @Override
    public String getUrl() {
        return qsd.path();
    }

    @Override
    public String getQueryString() {
        return qsd.rawQuery();
    }

    private String bodyString = null;

    @Override
    public String getBodyString() {
        if(bodyString == null) {
            bodyString = msg.content().toString(context.getCharset());
        }
        return bodyString;
    }

    @Override
    public boolean isMultipart() {
        return HttpPostRequestDecoder.isMultipart(msg);
    }

    @Override
    public NettyFormData getBodyFormData() {
        return new NettyFormData(this);
    }

    @Override
    public RestfulHttpMethod getMethod() {
        try {
            return RestfulHttpMethod.valueOf(msg.method().name());
        } catch (Throwable e) {
            return RestfulHttpMethod.REQUEST;
        }
    }

    @Override
    public String getHeader(String name) {
        return msg.headers().get(name);
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return channel.channel().remoteAddress();
    }

}
