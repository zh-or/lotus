package or.lotus.core.nio.http;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.support.RestfulHttpMethod;
import or.lotus.core.nio.*;
import or.lotus.core.nio.tcp.NioTcpSession;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;


public class HttpProtocolCodec implements ProtocolCodec {
    private static final String STATUS = "http-status";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String REQUEST = "http-request";
    private static final String HTTP_BODY = "http-body";
    private HttpServer context = null;
    private static final byte[] lineChars = "\r\n".getBytes();
    private static final byte[] headerChars = "\r\n\r\n".getBytes();
    public HttpProtocolCodec(HttpServer context) {
        this.context = context;
    }

    @Override
    public boolean decode(Session session, LotusByteBuf in, ProtocolDecoderOutput out) throws Exception {
        HttpStatus status = (HttpStatus) session.getAttr(STATUS);
        if (status == null) {
            session.setAttr(STATUS, HttpStatus.InitialLine);
            status = (HttpStatus) session.getAttr(STATUS);
        }
        int dataLength = in.getDataLength();
        switch (status) {
            case InitialLine:
                if(dataLength >= context.getMaxInitialLineLength()) {
                    throw new HttpServerException(431, "Request Header Fields Too Large");
                }
                if(in.search(lineChars) != -1) {
                    session.setAttr(STATUS, HttpStatus.Head);
                }
                break;
            case Head: {
                if(dataLength >= context.getMaxHeaderSize()) {
                    throw new HttpServerException(431, "Request Header Fields Too Large");
                }
                int headerEndPost = in.search(headerChars);
                if(headerEndPost == -1) {
                    //未接收到完整的http头
                   break;
                }
                //in.mark();
                byte[] headerBytes = new byte[headerEndPost];
                in.get(headerBytes);
                HttpRequest request = new HttpRequest(
                        context,
                        (NioTcpSession) session,
                        new String(headerBytes, context.getCharset())
                );

                if( request.method == RestfulHttpMethod.GET ||
                    request.method == RestfulHttpMethod.OPTIONS ||
                    request.method == RestfulHttpMethod.DELETE
                ) {
                    request.setAttribute(STATUS, HttpStatus.InitialLine);
                    out.write(request);
                    return true;
                }
                session.setAttr(REQUEST, request);
                final int contentLength = Utils.tryInt(request.getHeader(HttpHeaderNames.CONTENT_LENGTH), 0);

                session.setAttr(CONTENT_LENGTH, contentLength);
                if(contentLength > context.getMaxContentLength()) {
                    throw new HttpServerException(413, "Request Content Too Large");
                }
                if(contentLength <= 0) {
                    //没得body直接返回
                    return true;
                }
                HttpBodyData bodyData = new HttpBodyData(request, contentLength, contentLength > context.getCacheContentToFileLimit());
                request.setBodyData(bodyData);
                session.setAttr(HTTP_BODY, HttpStatus.Body);
                dataLength = in.getDataLength();
                //还存在数据则直接跳到下面body处理, 否则返回等待接收数据
                if(dataLength <= 0) {
                    return true;
                }
            }
            case Body: {
                final int contentLength = (Integer) session.getAttr(CONTENT_LENGTH, 0);
                final HttpRequest request = (HttpRequest) session.getAttr(REQUEST);
                HttpBodyData bodyData = request.getBodyFormData();
                if (contentLength > 0) {
                    //写入磁盘缓存或者内存缓存
                    if(bodyData.appendData((LotusByteBuffer) in)) {
                        //数据已经接收完毕
                        session.setAttr(STATUS, HttpStatus.InitialLine);
                        out.write(request);
                        session.removeAttr(REQUEST);
                    }
                } else {
                    session.setAttr(STATUS, HttpStatus.InitialLine);
                }
                return true;
            }
            //break;
        }

        return false;
    }

    @Override
    public boolean encode(Session session, Object msg, LotusByteBuf out) throws Exception {
        //是块对象
        if(msg instanceof HttpSyncResponse) {
            HttpSyncResponse syncResponse = (HttpSyncResponse) msg;
            if(syncResponse.buffer == null) {
                return true;
            }
            Charset charset = syncResponse.request.getContext().getCharset();
            //按块发送
            int len = syncResponse.buffer.getDataLength();
            String hexLen = Integer.toHexString(len);
            out.append(hexLen.getBytes(charset));
            out.append("\r\n".getBytes(charset));
            return true;
        }

        HttpResponse response = (HttpResponse) msg;
        if(!response.isSendHeader) {
            if(response.isOpenSync) {
                response.removeHeader(HttpHeaderNames.CONTENT_LENGTH);
            } else {
                response.setHeader(
                        HttpHeaderNames.CONTENT_LENGTH,
                        response.isFileResponse() ?
                                String.valueOf(response.getFile().length()) :
                                String.valueOf(response.writeBuffer.getDataLength())
                );
            }
            out.append(response.getHeaders().getBytes(response.charset));
            response.isSendHeader = true;
        }
        if(response.writeBuffer != null) {
            if(response.isOpenSync) {
                //按块发送
                int len = response.writeBuffer.getDataLength();
                String hexLen = Integer.toHexString(len);
                //len = len + 4 + hexLen.length();
                out.append(hexLen.getBytes(response.charset));
                out.append("\r\n".getBytes(response.charset));
            }

            ByteBuffer[] buffers = ((LotusByteBuffer) response.writeBuffer).getAllDataBuffer(true);
            for(ByteBuffer buff : buffers) {
                out.append(buff);
            }
        } else if(response.isFileResponse()) {
            File file = response.getFile();

            try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);) {
                long len = file.length();
                //把文件映射到内存发送, 这里最大不能超过Integer.MAX_LENGTH个字节即2G, 需要分成多个map映射
                do {
                    long step = Math.min(Integer.MAX_VALUE, len);
                    MappedByteBuffer mapBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, step);
                    mapBuffer.position((int) step);
                    out.append(mapBuffer);
                    len -= step;
                } while(len > 0);

                channel.close();
                return true;
            }
        }
        return true;

    }
}
