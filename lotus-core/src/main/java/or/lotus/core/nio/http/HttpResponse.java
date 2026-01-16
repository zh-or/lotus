package or.lotus.core.nio.http;

import or.lotus.core.common.Base64;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.RestfulResponse;
import or.lotus.core.http.restful.support.RestfulResponseStatus;
import or.lotus.core.nio.LotusByteBuffer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class HttpResponse extends RestfulResponse {
    protected HttpRequest request;
    protected boolean isSendHeader = false;
    protected boolean isOpenSync = false;
    protected LotusByteBuffer writeBuffer;
    protected HttpSyncResponse syncResponse;

    protected long[][] range = null;
    protected String boundary = null;

    public HttpResponse(HttpRequest request) {
        super(request);
        String connection = request.getHeader(HttpHeaderNames.CONNECTION);
        if(connection != null) {
            setHeader(HttpHeaderNames.CONNECTION, connection);
        } else {
            setHeader(HttpHeaderNames.CONNECTION, "keep-alive");
        }
        this.request = request;

        if(request.isWebSocket() && request.getContext().isEnableWebSocket()) {
            setStatus(RestfulResponseStatus.INFORMATIONAL_SWITCHING_PROTOCOLS);
            setHeader(HttpHeaderNames.UPGRADE, request.getHeader(HttpHeaderNames.UPGRADE));
            setHeader(HttpHeaderNames.CONNECTION, "Upgrade");
            String sec = request.getHeader(HttpHeaderNames.SEC_WEBSOCKET_KEY) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            try {
                sec = Base64.byteArrayToBase64(Utils.SHA1(sec));
            }catch(Exception e) {
                sec = "";
            }

            setHeader(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT, sec);
            //response.setHeader("Sec-WebSocket-Protocol", "");

        }
        String rangeHeader = request.getHeader(HttpHeaderNames.RANGE);
        if(rangeHeader != null) {
            status = RestfulResponseStatus.SUCCESS_PARTIAL_CONTENT;
            /*
            * test
            * 直接输出到控制台 curl -H "Range: bytes=1-" -v http://localhost:9999/朝闻道01.m4a -o -
            *
            * curl -H "Range: bytes=1-" -v http://localhost:9999/朝闻道01.m4a -o part.mp4
            * curl -H "Range: bytes=0-99" -v http://localhost:9999/朝闻道01.m4a -o part.mp4
            * curl -H "Range: bytes=-100" -v http://localhost:9999/朝闻道01.m4a -o part.mp4
            * curl -H "Range: bytes=0-9,20-29" -v http://localhost:9999/朝闻道01.m4a -o part.mp4
             * */

            //解析range
            int unitEnd = rangeHeader.indexOf("=");
            if(unitEnd == -1) {
                status = RestfulResponseStatus.CLIENT_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE;
                return;
            }
            rangeHeader = rangeHeader.substring(unitEnd + 1, rangeHeader.length());
            String[] rArr = Utils.splitManual(rangeHeader, ",");
            int len = rArr.length;
            range = new long[len][2];
            for(int i = 0; i < len; i++) {
                range[i] = new long[2];
                String[] rStr = Utils.splitManualEx(rArr[i], '-');
                if(rStr.length == 2) {
                    //如果第一部分没有表示发送最后一部分
                    range[i][0] = Utils.tryLong(rStr[0], -1);
                    //如果第二部分没有表示从第一部分到结尾
                    range[i][1] = Utils.tryLong(rStr[1], -1);

                    if(range[i][0] == -1 && range[i][1] == -1) {
                        status = RestfulResponseStatus.CLIENT_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE;
                        return;
                    }
                    if(len > 1 && (range[i][0] == -1 || range[i][1] == -1)) {
                        //大于1个范围时 不允许有空
                        status = RestfulResponseStatus.CLIENT_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE;
                        return;
                    }
                }
            }
            if(range.length > 1) {
                //浪费资源, 关闭之
                setHeader(HttpHeaderNames.CONNECTION, "close");
            }
            boundary = Utils.RandomString(10);
        } else {
            status = RestfulResponseStatus.SUCCESS_OK;
        }
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
        syncResponse = new HttpSyncResponse(request.session, request);
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
            writeBuffer = (LotusByteBuffer) request.getContext().server.pulledByteBuffer();
        }
    }

    public String getHeaders() {

        getFileLength();
        /*正常的文件范围请求, 并且范围正常*/
        if( status == RestfulResponseStatus.SUCCESS_PARTIAL_CONTENT &&
            range != null && range.length > 0 && isFileResponse()
        ) {

            if(range.length == 1) {
                StringBuilder sb = new StringBuilder();
                //                     起始-结束/文件总大小
                //Content-Range: bytes 900-999/1000
                if(range[0][0] == -1) {
                    if(range[0][1] <= 0 || range[0][1] >= fileLength) {
                        throw new HttpServerException(416, request, "Range Not Satisfiable");
                    }

                    sb.append("bytes ")
                            .append(fileLength - range[0][1])
                            .append('-')
                            .append(fileLength - 1)
                            .append('/')
                            .append(fileLength);

                    headers.put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(range[0][1])
                    );
                } else if(range[0][1] == -1) {

                    if(range[0][0] < 0 || range[0][0] >= fileLength - 1) {
                        throw new HttpServerException(416, request, "Range Not Satisfiable");
                    }
                    sb.append("bytes ")
                            .append(range[0][0])
                            .append('-')
                            .append(fileLength - 1)
                            .append('/')
                            .append(fileLength);
                    headers.put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(fileLength - range[0][0])
                    );
                } else {
                    if(range[0][0] >= range[0][1] || (range[0][1] - range[0][0]) > fileLength  || range[0][1] > fileLength - 1) {
                        throw new HttpServerException(416, request, "Range Not Satisfiable");
                    }
                    sb.append("bytes ")
                            .append(range[0][0])
                            .append('-')
                            .append(range[0][1])
                            .append('/')
                            .append(fileLength);
                    headers.put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(range[0][1] - range[0][0])
                    );
                }


                headers.put(HttpHeaderNames.CONTENT_RANGE,  sb.toString());

            }  else {
                //多个范围在编码的地方处理
                //在这里检查参数是否正确
                for(long[] range : range) {
                    if(range[0] > range[1] || range[1] > fileLength - 1) {
                        throw new HttpServerException(416, request, "Range Not Satisfiable");
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

        StringBuilder sb = new StringBuilder(request.getContext().getMaxHeaderSize());
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
        return sb.toString();
    }
}
