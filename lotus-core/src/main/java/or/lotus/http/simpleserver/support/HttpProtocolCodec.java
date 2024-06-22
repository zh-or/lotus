package or.lotus.http.simpleserver.support;

import or.lotus.http.simpleserver.HttpServer;
import or.lotus.nio.LotusIOBuffer;
import or.lotus.nio.ProtocolCodec;
import or.lotus.nio.ProtocolDecoderOutput;
import or.lotus.nio.Session;
import or.lotus.common.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;



public class HttpProtocolCodec implements ProtocolCodec {
    private static final String STATUS          =   "http-status";
    private static final String CONTENT_LENGTH  =   "content-length";
    private static final String REQUEST         =   "http-request";
    private HttpServer context                =   null;

    public HttpProtocolCodec(HttpServer context) {
        this.context = context;
    }

    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out)  throws Exception {
        HttpStatus status = (HttpStatus) session.getAttr(STATUS);
        if(status == null){
            session.setAttr(STATUS, HttpStatus.HEAD);
            status = (HttpStatus) session.getAttr(STATUS);
        }

        switch (status) {
            case HEAD:
            {
                /*if((context.getServerType() & HttpServer.SERVER_TYPE_HTTPS) > 0 &&  HttpsProtocolCodec.checkHTTPS(session, in, out)) {
                    HttpsProtocolCodec newdec = new HttpsProtocolCodec(context);
                    session.setAttr(STATUS, HttpsStatus.SHAKEHANDS);
                    session.setProtocolCodec(newdec);
                    return newdec.decode(session, in, out);
                }*/

                in.mark();
                while(in.remaining() > 3){
                    /*\r\n\r\n*/
                    if(in.get() == 13 && in.get() == 10 && in.get() == 13 && in.get() == 10){/*消息头完了*/
                        final byte[] bheaders = new byte[in.position()];
                        in.reset();
                        in.get(bheaders);
                        final HttpRequest req = new HttpRequest(session, context.getCharset(), context);
                        final String sheaders = new String(bheaders, context.getCharset());
                        req.parseHeader(sheaders);

                        final int contentLength = Utils.tryInt(req.getHeader("content-length"), 0);

                        if(contentLength > context.getRequestMaxLimit()) {
                            HttpResponse res = HttpResponse.defaultResponse(session, req);
                            res.setHeader("Connection", "close");
                            out.write(res);
                            session.closeOnFlush();
                            throw new Exception("content to overflow len:" + contentLength + " max:" + context.getRequestMaxLimit());
                        }

                        String contentType = req.getHeader("Content-Type");
                        if(contentType != null && contentType.indexOf("multipart/form-data") != -1) {//是文件上传请求
                            HttpFormData formData = new HttpFormData(req);
                            while(in.hasRemaining()) {
                                formData.write(in);
                            }
                            req.setFormData(formData);
                            if(contentLength <= formData.getCacheFileLength()) {
                                out.write(req);
                                session.setAttr(STATUS, HttpStatus.HEAD);
                                session.removeAttr(CONTENT_LENGTH);
                                session.removeAttr(REQUEST);
                                formData.close();
                                return true;
                            }
                            session.setAttr(STATUS, HttpStatus.FORMDATA);
                            session.setAttr(CONTENT_LENGTH, contentLength);
                            session.setAttr(REQUEST, req);
                            return false;
                        }
                        if(contentLength > 0){
                            if(contentLength <= in.remaining()){/*已经把body也收完了*/
                                final byte[] body = new byte[contentLength];
                                in.get(body);
                                req.setBody(body);
                                out.write(req);
                                return true;
                            }else{/*没有接收完, 等下次读事件触发再读*/
                                session.setAttr(STATUS, HttpStatus.BODY);
                                session.setAttr(CONTENT_LENGTH, contentLength);
                                session.setAttr(REQUEST, req);
                            }
                            return false;
                        }else{
                            out.write(req);
                            session.setAttr(STATUS, HttpStatus.HEAD);/*没有body*/
                            return true;
                        }
                    }
                }
                in.reset();
            }
                break;
            case BODY:
            {
                final int contentLength = (Integer) session.getAttr(CONTENT_LENGTH, 0);
                if(contentLength > 0){

                    if(contentLength <= in.remaining()){
                        HttpRequest req = (HttpRequest) session.removeAttr(REQUEST);
                        final byte[] body = new byte[contentLength];
                        in.get(body);
                        req.setBody(body);
                        out.write(req);
                        session.removeAttr(CONTENT_LENGTH);
                        session.setAttr(STATUS, HttpStatus.HEAD);
                        return true;
                    }else{
                        return false;
                    }
                }else{
                    session.setAttr(STATUS, HttpStatus.HEAD);
                }
            }
                break;
            case FORMDATA:
            {
                final int contentLength = (Integer) session.getAttr(CONTENT_LENGTH, 0);
                final HttpRequest req = (HttpRequest) session.getAttr(REQUEST);
                HttpFormData formData = req.getFormData();

                while(in.hasRemaining()) {
                    formData.write(in);
                }

                if(contentLength <= formData.getCacheFileLength()) {
                    formData.close();
                    session.setAttr(STATUS, HttpStatus.HEAD);
                    session.removeAttr(CONTENT_LENGTH);
                    session.removeAttr(REQUEST);
                    out.write(req);
                    return true;
                }

            }
                break;
        }

        return false;
    }

    @Override
    public boolean encode(Session session, Object msg, LotusIOBuffer out) throws Exception {
        HttpMessageWrap wrap = (HttpMessageWrap) msg;
        switch(wrap.type) {
            case HttpMessageWrap.HTTP_MESSAGE_TYPE_HEADER:
                out.append((ByteBuffer) wrap.data);
                return true;
            case HttpMessageWrap.HTTP_MESSAGE_TYPE_BUFFER:
                out.copyFromBuffer((LotusIOBuffer) wrap.data);
                return true;

            case HttpMessageWrap.HTTP_MESSAGE_TYPE_FILE:
                File file = (File) wrap.data;

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
