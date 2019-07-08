package lotus.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import lotus.http.WebSocketClient;
import lotus.http.WebSocketFrame;
import lotus.http.WebSocketClient.Handler;

public class WsTest {
    static WebSocketClient  ws = null;
    public static void main(String[] args) throws Exception, URISyntaxException {

        
        // System.out.println(Utils.getMid("123456789", "1", "3"));

        /**
         * 
         * ws://121.40.165.18:8800
         * 
         * wss://api.zb.cn/websocket
         * 
         * 必须加mask才可以通讯否者会直接断开连接
         */
        ws = WebSocketClient.connection(new URI("ws://121.40.165.18:8800"),
                
                 // new Proxy(Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080)),
                 
                new Handler() {
                    private ArrayList<WebSocketFrame> frames = new ArrayList<>();
                    @Override
                    public void onRecv(WebSocketClient ws, WebSocketFrame frame) {
                        System.out.println(frame);
                        /*frames.add(frame);
                        if(!frame.fin) {
                            return;
                        }
                       StringBuffer sb = new StringBuffer();
                        for(WebSocketFrame f : frames) {
                            sb.append(new String(f.body));
                        }
                        frames.clear();
                        System.out.println("recv:" + sb.toString());*/
                        // Utils.SLEEP(1100);
                        // ws.send(WebSocketFrame.text("date:" +
                        // System.currentTimeMillis()));
                        
                    }

                    @Override
                    public void onClose(WebSocketClient ws) {
                        System.out.println("连接断开?");
                    }

                    @Override
                    public void onConn(WebSocketClient ws1) {
                        System.out.println("连接完成...");
                        ws1.send(WebSocketFrame.text("{\"event\":\"addChannel\",\"channel\":\"markets\"}\n").mask());
                        //ws1.send(WebSocketFrame.text("{\"event\":\"addChannel\",\"channel\":\"trueqc_ticker\"}\n").mask());
                        /*new Thread(new Runnable() {
                            
                            @Override
                            public void run() {
                                while(true) {
                                    Utils.SLEEP(3000);
                                    ws.send(WebSocketFrame.text("{\"event\":\"addChannel\",\"channel\":\"zbqc_ticker\"}\n").mask());
                                }
                            }
                        }).start();*/
                        
                        
                    }
                    
                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                });
        // ws.send(WsRequest.text("test"));
        //ws.setIdeaTimeDiff(0);
    }
}
