package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.support.RestfulResponseStatus;
import or.lotus.core.nio.LotusByteBuffer;
import or.lotus.core.nio.Session;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class HttpResponse extends RestfulResponse {
    protected HttpServer context;
    protected boolean isSendHeader = false;
    protected boolean isOpenSync = false;
    protected LotusByteBuffer writeBuffer;
    protected HttpSyncResponse syncResponse;
    protected Session session;
    protected long[][] range = null;
    protected String boundary = null;


    public HttpResponse(HttpServer context, Session session) {
        this(context, session, RestfulResponseStatus.SUCCESS_OK);
    }

    public HttpResponse(HttpServer context, Session session, RestfulResponseStatus status) {
        super(context);
        this.context = context;
        this.session = session;
        this.status = status;
    }

    /** 开启异步发送, 异步发送必须调用 HttpSyncResponse.flush() 方法才会发送数据 */
    public HttpSyncResponse openSync() {
        if(file != null) {
            throw new RuntimeException("发送文件不支持再异步!");
        }
        if(syncResponse != null) {
            return syncResponse;
        }
        isOpenSync = true;
        //headers.put("Content-Encoding", "gzip");
        headers.put(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        syncResponse = new HttpSyncResponse(context, session);
        if(writeBuffer != null) {
            //buffer转移到异步response中
            syncResponse.buffer = writeBuffer;
            writeBuffer = null;
        }
        //统一在HttpServer.sendResponse 中发送response
        //request.session.write(this);
        return syncResponse;
    }

    public boolean isOpenSync() {
        return isOpenSync;
    }


    @Override
    public void flush() throws IOException {}

    @Override
    public void close() {
        if(writeBuffer != null) {
            writeBuffer.release();
            writeBuffer = null;
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        checkBuffer();
        writeBuffer.append(str.substring(off, off + len).getBytes(charset));
    }

    @Override
    public void write(int c) throws IOException {
        checkBuffer();
        writeBuffer.append(String.valueOf((char) c).getBytes(charset));
    }

    @Override
    public void write(char[] cBuf, int off, int len) throws IOException {

        checkBuffer();
        writeBuffer.append(String.valueOf(cBuf, off, len).getBytes(charset));
    }

    @Override
    public HttpResponse write(byte[] data) {
        checkBuffer();
        writeBuffer.append(data);
        return this;
    }

    @Override
    public HttpResponse clearWrite() {
        if(writeBuffer != null) {
            writeBuffer.clear();
        }
        return this;
    }

    private void checkBuffer() {
        if(isOpenSync) {
            throw new RuntimeException("启用异步返回后, 请使用 HttpSyncResponse 发送数据");
        }
        if(writeBuffer == null) {
            writeBuffer = (LotusByteBuffer) context.server.pulledByteBuffer();
        }
    }

    protected String headerStr = null;

    protected byte[] getHeaderBytes() {
        if(headerStr == null) {
            buildHeader(null);
        }
        return headerStr.getBytes(context.getCharset());
    }

    public void buildHeader(HttpRequest request) {

        getFileLength();
        /*正常的文件范围请求, 并且范围正常*/
        if( status == RestfulResponseStatus.SUCCESS_PARTIAL_CONTENT &&
            range != null && range.length > 0 && isFileResponse()
        ) {

            if(range.length == 1) {
                StringBuilder sb = new StringBuilder(1024);
                //                     起始-结束/文件总大小
                //Content-Range: bytes 900-999/1000
                if(range[0][0] == -1) {
                    if(range[0][1] <= 0 || range[0][1] >= fileLength) {
                        throw new HttpServerException(416, request, this, "Range Not Satisfiable:" + Arrays.toString(range));
                    }

                    sb.append("bytes ")
                            .append(fileLength - range[0][1])
                            .append('-')
                            .append(fileLength - 1)
                            .append('/')
                            .append(fileLength);

                    headers.put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(range[0][1]));
                } else if(range[0][1] == -1) {

                    if(range[0][0] < 0 || range[0][0] >= fileLength - 1) {
                        throw new HttpServerException(416, request, this, "Range Not Satisfiable:" + Arrays.toString(range));
                    }
                    sb.append("bytes ")
                            .append(range[0][0])
                            .append('-')
                            .append(fileLength - 1)
                            .append('/')
                            .append(fileLength);
                    headers.put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(fileLength - range[0][0]));
                } else {
                    if(range[0][0] >= range[0][1] || (range[0][1] - range[0][0]) > fileLength  || range[0][1] > fileLength - 1) {
                        throw new HttpServerException(416, request, this, "Range Not Satisfiable:" + Arrays.toString(range));
                    }
                    sb.append("bytes ")
                            .append(range[0][0])
                            .append('-')
                            .append(range[0][1])
                            .append('/')
                            .append(fileLength);
                    headers.put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(range[0][1] - range[0][0]));
                }


                headers.put(HttpHeaderNames.CONTENT_RANGE,  sb.toString());

            }  else {
                //多个范围在编码的地方处理
                //在这里检查参数是否正确
                for(long[] range : range) {
                    if(range[0] > range[1] || range[1] > fileLength - 1) {
                        throw new HttpServerException(416, request, this, "Range Not Satisfiable:" + Arrays.toString(range));
                    }
                }
            }

        } else {
            headers.put(
                    HttpHeaderNames.CONTENT_LENGTH,
                    isFileResponse() ?
                            String.valueOf(fileLength) :
                            String.valueOf(writeBuffer == null ? 0 : writeBuffer.getCountPosition())
            );
        }

        StringBuilder sb = new StringBuilder(context.getMaxHeaderSize());
        sb.append(status.line());
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, String> item = it.next();
            sb.append(item.getKey());
            sb.append(": ");
            sb.append(item.getValue());
            sb.append("\r\n");
        }
        sb.append("\r\n");
        headerStr = sb.toString();
    }
}
