package or.lotus.core.nio.http;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.support.RestfulHttpMethod;
import or.lotus.core.nio.*;
import or.lotus.core.nio.tcp.NioTcpSession;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;


public class HttpProtocolCodec implements ProtocolCodec {
    public static final String STATE = "http-state";
    public static final String REQUEST = "http-request";
    private HttpServer context;
    private static final byte[] lineChars = "\r\n".getBytes();
    private static final byte[] headerChars = "\r\n\r\n".getBytes();
    public HttpProtocolCodec(HttpServer context) {
        this.context = context;
    }

    @Override
    public boolean decode(Session session, LotusByteBuf in, ProtocolDecoderOutput out) throws Exception {
        HttpState state = (HttpState) session.getAttr(STATE);
        if (state == null) {
            session.setAttr(STATE, HttpState.InitialLine);
            state = (HttpState) session.getAttr(STATE);
        }
        int dataLength = in.getDataLength();
        switch (state) {
            case InitialLine://http协议第一行, 并验证第一行长度
                if(in.search(lineChars) == -1) {
                    if(dataLength >= context.getMaxInitialLineLength()) {
                        throw new HttpServerException(431, null, null, "Request Header Fields Too Large: " + dataLength);
                    }
                    break;
                }
                session.setAttr(STATE, HttpState.Head);
            case Head: {
                int headerEndPost = in.search(headerChars);
                if(headerEndPost == -1) {
                    if(dataLength >= context.getMaxHeaderSize()) {
                        session.setAttr(STATE, HttpState.InitialLine);
                        throw new HttpServerException(431, null, null, "Request Header Fields Too Large: " + dataLength);
                    }
                    //未接收到完整的http头
                   break;
                }
                //in.mark();
                byte[] headerBytes = new byte[headerEndPost + 4];
                in.get(headerBytes);
                HttpRequest request = new HttpRequest(
                        context,
                        (NioTcpSession) session,
                        new String(headerBytes, context.getCharset())
                );
                if(request.method == null) {
                    session.setAttr(STATE, HttpState.InitialLine);
                    throw new HttpServerException(431, request, null, "Method error");
                }
                if( request.method == RestfulHttpMethod.GET ||
                    request.method == RestfulHttpMethod.OPTIONS ||
                    request.method == RestfulHttpMethod.DELETE ||
                    request.contentLength <= 0 //有时候post也没得body直接返回
                ) {
                    session.setAttr(STATE, HttpState.InitialLine);
                    out.write(request);
                    if(request.contentLength > 0) {
                        //get 不允许携带body
                        request.headers.put(HttpHeaderNames.CONNECTION, "close");
                    }
                    return true;
                }
                if(request.contentLength > context.getMaxContentLength()) {
                    session.setAttr(STATE, HttpState.InitialLine);
                    throw new HttpServerException(413, request, null, "Request Content Too Large:" + request.contentLength);
                }
                session.setAttr(REQUEST, request);

                //如果在接收body时断开了连接, 需要处理HttpBodyData的释放: 在handler的close事件中关闭该request
                HttpBodyData bodyData = new HttpBodyData(request, request.contentLength > context.getCacheContentToFileLimit());
                request.setBodyData(bodyData);
                session.setAttr(STATE, HttpState.Body);
                dataLength = in.getDataLength();
                //还存在数据则直接跳到下面body处理, 否则返回等待接收数据
                if(dataLength <= 0) {
                    return true;
                }
            }
            case Body: {
                final HttpRequest request = (HttpRequest) session.getAttr(REQUEST);
                HttpBodyData bodyData = request.getBodyFormData();
                if (request.contentLength > 0) {
                    //写入磁盘缓存或者内存缓存
                    if(bodyData.appendData((LotusByteBuffer) in)) {
                        //数据已经接收完毕
                        session.setAttr(STATE, HttpState.InitialLine);
                        out.write(request);
                        session.removeAttr(REQUEST);
                    }
                } else {
                    session.setAttr(STATE, HttpState.InitialLine);
                }
                return true;
            }
            //break;
        }

        return false;
    }

    @Override
    public boolean encode(Session session, Object msg, EncodeOutByteBuffer out) throws Exception {
        //是块对象
        if(msg instanceof HttpSyncResponse) {
            HttpSyncResponse syncResponse = (HttpSyncResponse) msg;
            Charset charset = context.getCharset();
            int len = 0;
            if(syncResponse.buffer != null) {
                len = syncResponse.buffer.getCountPosition();
            }
            String hexLen = Integer.toHexString(len);
            out.append(hexLen.getBytes(charset));
            out.append(lineChars);
            if(len > 0) {
                out.append(syncResponse.buffer.getAllDataBuffer(true));
            } else if(syncResponse.buffer != null) {
                syncResponse.buffer.release();
            }
            out.append(lineChars);
            return true;
        }
        //除了块对象就是HttpResponse
        String oldContentType = null;
        HttpResponse response = (HttpResponse) msg;
        if(!response.isSendHeader) {
            if(response.isOpenSync) {
                response.removeHeader(HttpHeaderNames.CONTENT_LENGTH);
            }
            //处理多个range的头
            if(response.range != null && response.range.length > 1) {
                oldContentType = response.getContentType();
                if(Utils.CheckNull(oldContentType)) {
                    oldContentType = "multipart/byteranges";
                    response.setHeader(HttpHeaderNames.CONTENT_TYPE, "multipart/byteranges; boundary=" + response.boundary);
                } else {
                    //oldContentType = oldContentType + "; boundary=" + response.boundary;
                    response.setHeader(HttpHeaderNames.CONTENT_TYPE, "multipart/byteranges; boundary=" + response.boundary);
                }
                //response.setHeader(HttpHeaderNames.CONTENT_TYPE, oldContentType);
            }

            out.append(response.getHeaderBytes());
            response.isSendHeader = true;
        }
        if(response.writeBuffer != null) {
            if(response.isOpenSync) {
                //按块发送
                int len = response.writeBuffer.getDataLength();
                String hexLen = Integer.toHexString(len);
                //len = len + 4 + hexLen.length();
                out.append(hexLen.getBytes(response.charset));
                out.append(lineChars);
            }

            out.append(response.writeBuffer.getAllDataBuffer(true));
        } else if(response.isFileResponse()) {
            File file = response.getFile();
            long fileLength = response.getFileLength();
            if(response.range == null) {
                out.append(FileChannel.open(file.toPath(), StandardOpenOption.READ), 0, fileLength);
            } else {
                if(response.range.length == 1) {
                    if(response.range[0][0] == -1) {
                        out.append(
                                FileChannel.open(file.toPath(), StandardOpenOption.READ),
                                fileLength - response.range[0][1],
                                response.range[0][1]
                        );
                    } else if(response.range[0][1] == -1) {
                        out.append(
                                FileChannel.open(file.toPath(), StandardOpenOption.READ),
                                response.range[0][0],
                                fileLength - response.range[0][0]
                        );
                    } else {
                        out.append(
                                FileChannel.open(file.toPath(), StandardOpenOption.READ),
                                response.range[0][0],
                                response.range[0][1] - response.range[0][0]
                        );
                    }
                } else {
                    //参数问题在 HttpResponse.getHeaders 中验证过了
                    StringBuilder sb = new StringBuilder(100 + response.boundary.length());
                    for(long[] range : response.range) {
                        sb.append("--").append(response.boundary).append("\r\n");
                        sb.append(HttpHeaderNames.CONTENT_TYPE).append(": ")
                                .append(Utils.CheckNull(oldContentType) ? "multipart/byteranges" : oldContentType)
                                .append("\r\n");
                        sb.append(HttpHeaderNames.CONTENT_RANGE).append(": bytes ")
                                .append(range[0]).append('-')
                                .append(range[1])
                                .append('/')
                                .append(fileLength)
                                .append("\r\n").append("\r\n");
                        out.append(sb.toString().getBytes(response.charset));
                        sb.setLength(0);
                        out.append(
                                FileChannel.open(file.toPath(), StandardOpenOption.READ),
                                range[0],
                                range[1] - range[0]
                        );
                        out.append("\r\n".getBytes());
                    }
                    sb.append("--").append(response.boundary).append("--").append("\r\n");
                    out.append(sb.toString().getBytes(response.charset));
                }
            }

             /*try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);) {
                //channel.transferTo(0, file.length(), writableByteChannel);

               long len = file.length();
                //把文件映射到内存发送, 这里最大不能超过Integer.MAX_LENGTH个字节即2G, 需要分成多个map映射
                do {
                    long step = Math.min(Integer.MAX_VALUE, len);
                    MappedByteBuffer mapBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, step);
                    mapBuffer.position((int) step);
                    out.append(mapBuffer);
                    len -= step;
                } while(len > 0);

                //channel.close();
                return true;
            }*/
        }
        return true;

    }



}
