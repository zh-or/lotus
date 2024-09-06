package or.lotus.core.http.netty;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import or.lotus.core.http.restful.RestfulRequest;

import java.io.File;

public abstract class NettyFileFilter {
    /** 是否拦截, 返回true 表示拦截文件请求, 未处理文件是否存在等 */
    public boolean before(FullHttpRequest request) {
        return false;
    }

    /** 是否拦截, 返回true 表示拦截文件请求, 请求&本地文件 */
    public boolean request(File file, FullHttpRequest request, HttpResponse response) {
        return false;
    }
}
