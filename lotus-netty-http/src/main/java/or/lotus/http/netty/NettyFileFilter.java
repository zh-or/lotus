package or.lotus.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.io.File;

public abstract class NettyFileFilter {
    /** 是否拦截, 返回true 表示拦截文件请求, 此处拦截会返回404给客户端, 未处理文件是否存在等 */
    public boolean before(FullHttpRequest request) {
        return false;
    }

    /** 是否拦截, 返回true 表示拦截文件请求, 请求&本地文件 */
    public boolean request(File file, FullHttpRequest request, HttpResponse response) {
        return false;
    }

    /**处理本地文件之前调用, 如果此方法返回了file对象则直接返回给客户端*/
    public File getFile(String path, ChannelHandlerContext ctx, FullHttpRequest request) {
        return null;
    }
}
