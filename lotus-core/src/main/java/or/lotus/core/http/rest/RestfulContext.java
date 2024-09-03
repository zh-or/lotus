package or.lotus.core.http.rest;

import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.rest.ann.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RestfulContext {
    static final Logger log = LoggerFactory.getLogger(RestfulContext.class);
    protected InetSocketAddress bindAddress;
    protected static final Charset charset = Charset.forName("utf-8");

    /**key = @Bean->value | packageName*/
    protected ConcurrentHashMap<String, Object> beansCache;

    /** key = url*/
    protected ConcurrentHashMap<String, RestfulDispatcher> dispatcherConcurrentHashMap;

    /**请求过滤器*/
    protected RestfulFilter filter;

    /** 处理请求业务线程池大小 */
    protected int eventThreadPoolSize = 20;

    /** 可访问静态文件目录 */
    protected Path staticPath = null;

    /** 当有设置静态文件目录时, 是否开启自动列出文件功能 */
    protected boolean enableDirList = false;

    /** request & response buffer初始大小 */
    protected int bufferSize = 1024 * 4;

    /** 最大允许的请求体大小 */
    protected int maxContentLength = 1024 * 1024 * 2;

    /** 当请求路径为 / 时, 默认读取的文件, 此设置只能和 dirList 二选一 */
    protected String defaultIndexFile = null;

    protected boolean isEnableGZIP = false;

    public RestfulContext() {
        beansCache = new ConcurrentHashMap<>();
        dispatcherConcurrentHashMap = new ConcurrentHashMap<>();
    }

    public void start(int port) {
        start(new InetSocketAddress(port));
    }

    public void start(String host, int port) {
        start(new InetSocketAddress(host, port));
    }

    public void start(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
        onStart();
    }

    protected void stop() {
        onStop();
    }

    protected abstract void onStart();
    protected abstract void onStop();

    /**扫描指定包名并添加拥有@RestfulController注解的类为Controller*/
    public int scanController(String packageName) throws URISyntaxException, IOException, ClassNotFoundException {
        Utils.assets(packageName, "包名不能为空");
        List<String> clazzs = BeanUtils.getClassPathByPackage(packageName);

        for (String path : clazzs) {
            Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(path);
            RestfulController annotation = c.getAnnotation(RestfulController.class);
            if(annotation != null) {
                //只加载有注解的类
                String url1 = annotation.value();

                Method[] methods = c.getMethods();

                for(Method m : methods) {
                    RestfulDispatcher dispatcher = null;
                    if(m.isAnnotationPresent(Get.class)) {
                        Get get = m.getAnnotation(Get.class);
                        dispatcher = new RestfulDispatcher(url1 + get.value(), c, m, RestfulHttpMethod.GET, get.isPattern());
                    } else if(m.isAnnotationPresent(Post.class)) {
                        Post post = m.getAnnotation(Post.class);
                        dispatcher = new RestfulDispatcher(url1 + post.value(), c, m, RestfulHttpMethod.POST, post.isPattern());
                    } else if(m.isAnnotationPresent(Put.class)) {
                        Put put = m.getAnnotation(Put.class);
                        dispatcher = new RestfulDispatcher(url1 + put.value(), c, m, RestfulHttpMethod.PUT, put.isPattern());
                    } else if(m.isAnnotationPresent(Delete.class)) {
                        Delete delete = m.getAnnotation(Delete.class);
                        dispatcher = new RestfulDispatcher(url1 + delete.value(), c, m, RestfulHttpMethod.DELETE, delete.isPattern());
                    }

                    if(dispatcher != null) {
                        dispatcherConcurrentHashMap.put(dispatcher.url, dispatcher);
                    }
                }

                log.trace("加载Controller {} => {}", url1, path);
            }
        }

        return 0;
    }

    /**执行参数中beans的带有Bean注解的方法, 并将返回值缓存, 在Controller中使用Autowired注解时自动注入该缓存*/
    public int addBean(Object beans) {
        return 0;
    }


    public void setStaticPath(Path staticPath) {
        this.staticPath = staticPath;
    }

    public void setEnableDirList(boolean enableDirList) {
        this.enableDirList = enableDirList;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public void setDefaultIndexFile(String defaultIndexFile) {
        this.defaultIndexFile = defaultIndexFile;
    }

    public void setEnableGZIP(boolean enableGZIP) {
        isEnableGZIP = enableGZIP;
    }

    public void setEventThreadPoolSize(int eventThreadPoolSize) {
        this.eventThreadPoolSize = eventThreadPoolSize;
    }
}
