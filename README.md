
## 自己造的轮子
1. 现有框架都太重了, 一堆依赖
2. 自己能很方便的增加功能

## 包含工具库
1. 高并发轻量级NIO框架
2. 轻量级HTTP服务器(基于NIO)
3. 轻量级WebSocket客户端
4. Android JSON库copy
5. Android SparseArray库copy

## todo
1. `HTTPClient`实现
2. `HttpServer`的`Keep-Alive`实现完善
3. `HttpServer`异常处理完善
4. `HttpServer` 的`HTTP Pipelining` 实现 (不是必须)

### NIO服务器
```
NioTcpServer server = new NioTcpServer();
server.setProtocolCodec(new LengthProtocolCode());
server.setHandler(new IoHandler() {
  @Override
  public void onConnection(Session session) {
    System.out.println("连接:" + session.getRemoteAddress());
  }
  @Override
  public void onClose(Session session) {
    System.out.println("断开:" + session.getRemoteAddress());
  }
  
  @Override
  public void onRecvMessage(Session session, Object obj) {
    byte[] data = (byte[]) obj;
    System.out.println("收到数据长度:" + data.length);
    session.write("ok".getBytes());
  }
});
server.start(new InetSocketAddress(9000));
```

### HTTP服务器

原生使用
```
HttpServer httpServer = new HttpServer();
httpServer.setCharset(Charset.forName("utf-8"));
httpServer.enableWebSocket(true);//启用websocket支持
httpServer.setEventThreadPoolSize(10);
httpServer.setReadBufferCacheSize(1024 * 4);
httpServer.setHandler(new HttpHandler() {
  @Override
  public void wsConnection(Session session, HttpRequest request) throws Exception {
    System.out.println("websocket 连接");
  }

  @Override
  public void wsClose(Session session, HttpRequest request) throws Exception {
    System.out.println("websocket 断开");
  }

  @Override
  public void wsMessage(Session session, HttpRequest request, WebSocketFrame frame) throws Exception {
    System.out.println("websocket 消息");
    session.write(WebSocketFrame.text(frame.getText()));//原样发回
  }
  @Override
  public void service(HttpMethod methed, HttpRequest request, HttpResponse response){
    System.out.println("收到HTTP请求");
    response.write("收到请求:" +  + methed.toString());
    if(request.isFormData()) {//FormData 请求
      HttpFormData formData = request.getFormData();
      response.write("收到文件上传:" + formData.getCacheFileLength() + "<br/>");
      //上传文件会解析到系统临时文件夹内, 如需使用请移动到自己的目录, 否则会被清理掉
      response.write("name:" +  formData.getCacheFile().getAbsolutePath());
      response.write("file:" +  formData.getFile("file"));//File
      response.write("files:" +  formData.getFile("files"));// File[]
      
    }
  }
});
server.start(new InetSocketAddress(9000));
```
---
注解用法
```
HttpServer server = new HttpServer();
server.setCharset(Charset.forName("utf-8"));
server.setEventThreadPoolSize(10);
server.setReadBufferCacheSize(1024 * 4);
HttpRestServiceDispatcher dispathcer = new HttpRestServiceDispatcher();
dispathcer.addService(new UserService());
server.setHandler(dispathcer);
server.start(new InetSocketAddress(9000));
```

UserService.java
```
@HttpServicePath(path="/user")
public class UserService extends HttpBaseService {

    @HttpServiceMap(path="/login")
    public void login(HttpRequest request, HttpResponse response) {
      //请求路径为 http://host:port/user/login
      System.out.println("登录调用");
      response.write("收到登录请求");
    }
}
```
启用HTTPS支持
```
server.setKeyStoreAndEnableSSL("./system/test.keystore", "123456789");
```


[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)
[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg)](https://github.com/996icu/996.ICU/blob/master/LICENSE)
