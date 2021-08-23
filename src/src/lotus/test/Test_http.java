package lotus.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import lotus.http.WebSocketFrame;
import lotus.http.server.HttpHandler;
import lotus.http.server.HttpServer;
import lotus.http.server.support.HttpMethod;
import lotus.http.server.support.HttpRequest;
import lotus.http.server.support.HttpResponse;
import lotus.log.Log;
import lotus.nio.Session;

public class Test_http {
    static HttpServer httpserver;
    static Log        log = Log.getInstance();
    enum Test{
        A(1),
        B(2),
        C(3);
        int type;
        Test(int num){this.type = num;}
    }
    public static void main(String[] args) throws IOException, URISyntaxException {
        URI uri = new URI("ws://a.com/xx?b=1");

        System.out.println(uri.getScheme());
        System.out.println(uri.getHost());
        System.out.println(uri.getPort());
        System.out.println(uri.getPath());
        System.out.println(uri.getQuery());
        
        System.out.println("---------------------------------------");
        
        Test t = Test.valueOf("B");
        System.out.println(t + " " + t.type);
        byte[] bytes = new byte[]{0x01, 0x02, 0x03};
        ByteBuffer buff = ByteBuffer.allocate(1024);
        buff.put(bytes);
        
        
        System.out.println(String.format(
                "pos:%d, limit:%d, cap:%d, remaining:%d, size:%d", 
                buff.position(), 
                buff.limit(), 
                buff.capacity(),
                buff.remaining(),
                buff.capacity() - buff.remaining()
                ));
        //System.exit(0);
        httpserver = new HttpServer();
        httpserver.enableWebSocket(true);
        httpserver.setHandler(new HttpHandler() {
            
            @Override
            public void wsConnection(Session session, HttpRequest request) throws Exception {
                log.info("wsConnection, request: %s", request.toString());
            }
            
            @Override
            public void wsClose(Session session, HttpRequest request) throws Exception {
                log.info("wsClose, request: %s", request.toString());
            }
            
            @Override
            public void wsMessage(Session session, HttpRequest request, WebSocketFrame frame) throws Exception {
                switch(frame.opcode) {
                    case WebSocketFrame.OPCODE_PING:
                    case WebSocketFrame.OPCODE_CLOSE:
                        return;
                }
                log.info("wsMessage, frame:%s", frame.toString());
                
                session.write(WebSocketFrame.text(new String(frame.getBinary())));
            }
            
            @Override
            public void service(HttpMethod mothed, HttpRequest request, HttpResponse response) {
                log.info("http request: %s", request.toString());
                StringBuffer sb = new StringBuffer(1024 * 5);
                for(int i = 0; i < sb.capacity(); i++) {
                    sb.append("x");
                }
                //response.write(sb.toString());
                response.write("中文");
            }
        });
        httpserver.start(new InetSocketAddress(8090));
        log.info("启动完成...");
    }
    

}
