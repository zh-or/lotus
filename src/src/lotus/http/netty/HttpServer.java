package lotus.http.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
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

    private ArrayList<HttpRestServiceHook> hooks;
    private ConcurrentHashMap<String, HttpBaseService> services;
    private HttpRestErrorHandler errorHandler = null;

    /**
     * 默认静态文件访问文件夹为 ./static
     */
    public HttpServer() {
        services = new ConcurrentHashMap<String, HttpBaseService>();
        hooks = new ArrayList<>();
        staticPath = Paths.get("./static");
        charset = Charset.forName("utf-8");


        /**
         * HttpStaticFileServerHandler 要加hook
         * HttpStaticFileServerHandler 代码需要精简
         * https 要再测试一下
         * 优化api
         */
    }

    public synchronized void addServiceHook(HttpRestServiceHook hook) {
        hooks.add(hook);
    }

    public synchronized void removeServiceHook(HttpRestServiceHook hook) {
        hooks.remove(hook);
    }

    public ArrayList<HttpRestServiceHook> getFilters() {
        return hooks;
    }

    public void setEventThreadTotal(int total) throws Exception {
        checkIsRun();
        if(eventExec != null) {
            eventExec.shutdown();
        }
        eventExec = Executors.newFixedThreadPool(total, (run) -> new Thread(run, "lotus http server event thread pool"));
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

    public ConcurrentHashMap<String, HttpBaseService> getServices() {
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

    public Object exception(Throwable e, HttpRequestPkg request) {
        if(errorHandler != null) {
            return errorHandler.exception(e, request);
        } else {
            e.printStackTrace();
            return null;
        }
    }

    public void addHttpService(HttpBaseService service) {
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
        services.put(base, service);
    }

    public void setKeyStoreAndEnableSSL(String keystore, String password) throws Exception {
        checkIsRun();
        sslContext = SslContextBuilder.forServer(new File(keystore), null, password).build();
    }

    public void start() {
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
                        pipeline.addLast("codec", new HttpServerCodec());// HTTP 编解码
                        if(isEnableGZIP) {
                            pipeline.addLast("compressor", new HttpContentCompressor());// HttpContent 压缩
                        }
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536)); // HTTP 消息聚合
                        pipeline.addLast("handler", new HttpServerHandler(HttpServer.this));
                        pipeline.addLast("handler2", new HttpStaticFileServerHandler(HttpServer.this));
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

    public void bind(InetSocketAddress address) throws InterruptedException {

        Channel ch = serverBootstrap.bind(address).sync().channel();
    }
}
