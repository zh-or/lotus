package or.lotus.core.http.netty;

import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import or.lotus.core.http.restful.RestfulFormData;
import or.lotus.core.http.restful.RestfulFormDataItem;

public class NettyFormData extends RestfulFormData {
    //解析文件大小(如果是:minSize则会过滤掉16K以下的文件,这个则不限制文件最小长度)
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE);


    NettyRequest request;
    HttpPostRequestDecoder decoder;

    public NettyFormData(NettyRequest request) {
        this.request = request;
        decoder = new HttpPostRequestDecoder(factory, request.rawRequest());
    }

    @Override
    public RestfulFormDataItem getFormDataItem(String name) {
        if(request.isMultipart()) {
            InterfaceHttpData item = decoder.getBodyHttpData(name);
            if(item != null) {
                return new NettyFormDataItem(item);
            }
        }

        return null;
    }
}
