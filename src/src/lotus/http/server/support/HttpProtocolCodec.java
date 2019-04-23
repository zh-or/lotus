package lotus.http.server.support;

import java.nio.ByteBuffer;

import lotus.http.server.HttpRequest;
import lotus.http.server.HttpServer;
import lotus.http.server.HttpStatus;
import lotus.nio.ProtocolCodec;
import lotus.nio.ProtocolDecoderOutput;
import lotus.nio.Session;
import lotus.utils.Utils;

public class HttpProtocolCodec implements ProtocolCodec{
    private static final String STATUS          =   "http-status";
    private static final String CONTENT_LENGTH  =   "content-length";
    private static final String REQUEST         =   "http-request";
    private HttpServer context                  =   null;
    
    public HttpProtocolCodec(HttpServer context) {
        this.context = context;
    }
    
    @Override
    public boolean decode(Session session, ByteBuffer in, ProtocolDecoderOutput out)  throws Exception{
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
                        final int contentLength = Utils.StrtoInt(req.getHeader("content-length"));
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
                        session.setAttr(STATUS, HttpStatus.HEAD);//不知道为什么以前这里写漏了???
                        return true;
                    }else{
                        return false;
                    }
                }else{
                    session.setAttr(STATUS, HttpStatus.HEAD);
                }
            }
                break;
        }
        
        return false;
    }

    @Override
    public ByteBuffer encode(Session session, Object msg) throws Exception {
        return (ByteBuffer) msg;
    }
}
