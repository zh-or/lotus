package or.lotus.common.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    int responseBufferSize = 1024 * 4;
    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    ServerBootstrap serverBootstrap = null;
    SslContext sslContext = null;
    ExecutorService eventExec = null;

    Path staticPath = null;

    Charset charset;
    boolean isRun = false;
    boolean isEnableGZIP = false;

    boolean isEnableDirList = false;

    int maxContentLength = 65536;

    //输出从创建 ModelAndView 到解析完毕模板的时间
    boolean outModelAndViewTime = true;

    //当访问路径为 / 时自动尝试的文件名
    String defaultIndexFile = "/index.html";

    String templateDir = null;
    TemplateEngine templateEngine = null;
    FileTemplateResolver templateResolver = null;

    private HashMap<String, WebSocketMessageHandler> webSocketHandlers;
    private ArrayList<HttpRestServiceHook> hooks;
    private HttpStaticFileHook staticFileHook;
    private ConcurrentHashMap<String, HttpServiceWrap> services;
    private HttpRestErrorHandler errorHandler = null;

    /**
     * 默认静态文件访问文件夹为 ./static
     * 默认 / 访问默认文件为 index.html
     * 如果所访问路径未被 HttpServicePath 注册, 则会检查是否存在静态文件, 并且如果路径无后缀时会自动加上 .html 尝试
     * 支持 thymeleaf 解析模板引擎, 并且 ModelAndView 支持输出从创建对象到模板渲染结束的时间
     */
    public HttpServer() {
        services = new ConcurrentHashMap<String, HttpServiceWrap>();
        webSocketHandlers = new HashMap<>();
        hooks = new ArrayList<>();
        staticPath = Paths.get("./static");
        charset = Charset.forName("utf-8");
    }

    public void setBufferSize(int size) {
        responseBufferSize = size;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public void setDefaultIndexFile(String defaultIndexFile) {
        this.defaultIndexFile = defaultIndexFile;
    }

    public void setOutModelAndViewTime(boolean outModelAndViewTime) {
        this.outModelAndViewTime = outModelAndViewTime;
    }

    public synchronized void enableTemplateEngine(String path, boolean cache) throws Exception {
        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix(path);
        templateResolver.setSuffix(".html");
        templateResolver.setCacheable(cache);
        templateResolver.setCharacterEncoding("UTF-8");

        enableTemplateEngine(templateResolver);
    }
    public synchronized void enableTemplateEngine(FileTemplateResolver templateResolver) throws Exception {
        checkIsRun();
        this.templateDir = templateResolver.getPrefix();
        this.templateResolver = templateResolver;
    }

    public synchronized void addServiceHook(HttpRestServiceHook hook) {
        hooks.add(hook);
    }

    public synchronized void removeServiceHook(HttpRestServiceHook hook) {
        hooks.remove(hook);
    }

    public ArrayList<HttpRestServiceHook> getHooks() {
        return hooks;
    }

    public void setStaticFileHook(HttpStaticFileHook hook) {
        staticFileHook = hook;
    }

    public HttpStaticFileHook getStaticFileHook() {
        return staticFileHook;
    }

    public void setEventThreadTotal(int total) throws Exception {
        checkIsRun();
        if(eventExec != null) {
            eventExec.shutdown();
        }
        eventExec = Executors.newFixedThreadPool(total, (run) -> new Thread(run, "service-thread-pool"));
    }

    public boolean isEnableDirList() {
        return isEnableDirList;
    }

    public void setEnableDirList(boolean enableDirList) {
        isEnableDirList = enableDirList;
    }

    public void enableGZIP() {
        isEnableGZIP = true;
    }

    public void disableGZIP() {
        isEnableGZIP = false;
    }

    public void setCharset(Charset charset) throws Exception {
        checkIsRun();
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }

    public ConcurrentHashMap<String, HttpServiceWrap> getServices() {
        return services;
    }

    public void setStaticPath(Path path) throws Exception {
        checkIsRun();
        this.staticPath = path;
    }

    public void setErrorHandler(HttpRestErrorHandler handler) throws Exception {
        checkIsRun();
        errorHandler = handler;
    }

    public void setMaxContentLength(int maxContentLength) throws Exception {
        checkIsRun();
        this.maxContentLength = maxContentLength;
    }

    public void addWebSocketHandler(WebSocketMessageHandler handler) {
        webSocketHandlers.put(handler.getPath(), handler);
    }

    public WebSocketMessageHandler getWebSocketMessageHandler(String path) {
        return webSocketHandlers.get(path);
    }

    public Object exception(Throwable e, HttpRequestPkg request) {
        if(errorHandler != null) {
            return errorHandler.exception(e, request);
        } else {
            e.printStackTrace();
            return null;
        }
    }

    public void addHttpService(HttpBaseService service) {
        if(service == null) {
            return ;
        }
        String base = "/";
        try{
            service.__setContext(this);
            HttpServicePath path = service.getClass().getAnnotation(HttpServicePath.class);
            if(path != null) {
                base = path.value();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        HttpServiceWrap wrap = services.get(base);
        if(wrap == null) {
            wrap = new HttpServiceWrap(base);
        }
        wrap.addService(service);
        services.put(base, wrap);
    }

    public void setKeyStoreAndEnableSSL(String keystore, String password) throws Exception {
        checkIsRun();
        sslContext = SslContextBuilder.forServer(new File(keystore), null, password).build();
    }

    public void start() {
        if(templateResolver != null) {
            //开启模板引擎
            templateEngine = new TemplateEngine();
            templateEngine.setTemplateResolver(templateResolver);
        }

        //accept thread group
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);
        //io thread group
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        //serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (sslContext != null) {
                            pipeline.addLast(sslContext.newHandler(socketChannel.alloc()));
                        }
                        pipeline.addLast("httpCodec", new HttpServerCodec());// HTTP 编解码
                        if(isEnableGZIP) {
                            pipeline.addLast("decompressor", new HttpContentDecompressor());// HttpContent 解压缩
                            pipeline.addLast("compressor", new HttpContentCompressor());// HttpContent 压缩
                        }
                        pipeline.addLast("aggregator", new HttpObjectAggregator(HttpServer.this.maxContentLength)); // HTTP 消息聚合
                        if(HttpServer.this.webSocketHandlers.size() > 0) {
                            //pipeline.addLast("websocketDecoder", new WebSocketServerProtocolHandler());//处理websocket请求
                            pipeline.addLast("websocketCompressor", new WebSocketServerCompressionHandler());

                            for(Map.Entry<String, WebSocketMessageHandler> entry : webSocketHandlers.entrySet()) {
                                WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                                        .checkStartsWith(true)//值检查url的前面是否匹配, 不然的话不能在url上传参数
                                        .allowExtensions(true)
                                        .handleCloseFrames(true)
                                        //.allowMaskMismatch(false)//mask
                                        .websocketPath(entry.getValue().getPath())
                                        .build();

                                //处理websocket请求
                                pipeline.addLast("websocketDecoder-" + entry.getKey(), new WebSocketServerProtocolHandler(config));
                            }
                            pipeline.addLast("websocketHandler", new HttpWebSocketServerHandler(HttpServer.this));//处理websocket请求
                        }

                        pipeline.addLast("apiHandler", new HttpServerHandler(HttpServer.this));//处理api 请求
                        pipeline.addLast("staticFileHandler", new HttpStaticFileServerHandler(HttpServer.this));//处理静态文件请求
                    }

                });
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        isRun = true;
    }

    public void stop() {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            if(eventExec != null) {
                eventExec.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRun = false;
    }

    private void checkIsRun() throws Exception {
        checkIsRun("请在start之前调用");
    }

    private void checkIsRun(String msg) throws Exception {
        if(isRun) {
            throw new Exception(msg);
        }
    }

    public void exec(Runnable run) {
        if(eventExec == null) {
            run.run();
        } else {
            eventExec.execute(run);
        }
    }

    public void bind(InetSocketAddress address) throws Exception {
        if(serverBootstrap == null || !isRun) {
            throw new Exception("请先调用start()");
        }
        Channel ch = serverBootstrap.bind(address).sync().channel();
    }
}
