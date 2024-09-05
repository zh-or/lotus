package or.lotus.core.http.restful;

import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.*;
import or.lotus.core.http.restful.support.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** 使用方式:
 * 1. http容器继承此类, 并实现onStart()和onStop()方法实现启动停止
 * 2. 实现 sendResponse 处理返回内容
 * 2. 调用addBeans添加 bean
 * 3. 调用scanController扫描Controller类, 并实现@RestfulController注解
 * 4. http容器收到请求时调用dispatch方法分发请求, 返回值为需要输出的内容
 *  */
public abstract class RestfulContext {
    protected static final Logger log = LoggerFactory.getLogger(RestfulContext.class);
    protected InetSocketAddress bindAddress;

    /**key = @Bean->value | packageName*/
    protected ConcurrentHashMap<String, Object> beansCache;

    /** key = url + http-method-str*/
    protected ConcurrentHashMap<String, RestfulDispatcher> dispatcherAbsMap;
    protected ArrayList<RestfulDispatcher> dispatcherPatternList;

    /** 业务线程池 */
    protected ExecutorService executorService;

    protected boolean isRunning = false;

    /**请求过滤器*/
    protected RestfulFilter filter;


    public RestfulContext() {
        beansCache = new ConcurrentHashMap<>();
        dispatcherAbsMap = new ConcurrentHashMap<>();
        dispatcherPatternList = new ArrayList<>();
    }

    public void start(int port) throws InterruptedException {
        start(new InetSocketAddress(port));
    }

    public void start(String host, int port) throws InterruptedException {
        start(new InetSocketAddress(host, port));
    }

    public synchronized void start(InetSocketAddress bindAddress) throws InterruptedException {
        if(isRunning) {
            throw new RuntimeException("服务已启动");
        }
        this.bindAddress = bindAddress;
        if(eventThreadPoolSize > 0) {
            executorService = Executors.newFixedThreadPool(
                    eventThreadPoolSize,
                    (run) -> new Thread(run, "lotus-restful-service-pool")
            );
        }
        onStart();
        isRunning = true;
    }

    public synchronized void stop() {
        onStop();
        if(executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("等待业务线程池退出失败:", e);
            }
        }
        isRunning = false;
    }

    protected abstract void onStart() throws InterruptedException;
    protected abstract void onStop();

    /**
     * 业务处理完毕后会调用此方法发送返回
     * 如果开启线程池处理业务, 那么此回调则是在业务线程池中执行
     * @param isHandle 表示是否已经处理该请求
     * */
    protected abstract void sendResponse(boolean isHandle, RestfulRequest request, RestfulResponse response);

    /** 分发请求 */
    protected void dispatch(RestfulRequest request, RestfulResponse response) {
        Runnable run = () -> {
            /**
             * 请求处理流程并调用controller
             * 1. filter->beforeRequest 过滤器
             * 2. 普通全字匹配 controller 匹配并调用
             * 3. 正则url controller匹配并调用
             * */
            try {
                if(filter != null && filter.beforeRequest(request, response)) {
                    sendResponse(true, request, response);
                    return ;
                }
                // 普通请求
                RestfulDispatcher dispatcher = getDispatcher(request);
                if(dispatcher != null) {
                    dispatcher.dispatch(this, request, response);
                    if(filter != null) {
                        filter.afterRequest(request, response);
                    }
                    sendResponse(true, request, response);
                    return;
                }
                //未匹配, 未处理当前请求
                sendResponse(false, request, response);

            } catch (Throwable e) {
                if(filter != null && filter.exception(e, request, response)) {
                    sendResponse(true, request, response);
                } else {
                    response.setStatus(RestfulResponseStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
                    response.clearWrite();
                    try {
                        response.write(e.toString());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    sendResponse(true, request, response);
                    log.error("处理请求出错:", e);
                }
            }
        };

        if(executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                run.run();
            });
        } else {
            run.run();
        }
    }

    protected RestfulDispatcher getDispatcher(RestfulRequest request) {
        // 普通请求
        RestfulDispatcher dispatcher = dispatcherAbsMap.get(request.getDispatchUrl());
        if(dispatcher != null) {
            return dispatcher;
        }

        //正则url验证
        for(RestfulDispatcher patternDispatcher : dispatcherPatternList) {
            if(patternDispatcher.checkPattern(request)) {
                return patternDispatcher;
            }
        }

        return null;
    }

    /** 扫描指定包名并添加拥有 @RestfulController 注解的类为 Controller, 返回扫描到的 Controller 数量
     * 此方法需要在 addBeans 后调用, 否则无法正确使用 @Autowired 注入
     * 注意 Controller 类只会在加载时创建一次, 并不会每次调用时都创建
     * */
    public int scanController(String packageName) throws URISyntaxException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if(isRunning) {
            /** 因为正则的 url 列表使用的 ArrayList, 不是线程安全的, 后面有需要再去除此限制 */
            throw new RuntimeException("启动后不支持再添加 Controller");
        }
        Utils.assets(packageName, "包名不能为空");
        List<String> clazzs = BeanUtils.getClassPathByPackage(packageName);

        for (String path : clazzs) {
            Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(path);
            RestfulController annotation = c.getAnnotation(RestfulController.class);
            if(annotation != null) {
                //controller 只创建一次
                Object controller = c.getDeclaredConstructor().newInstance();

                //注入bean
                RestfulUtils.injectBeansToObject(this, controller);

                //只加载有注解的类
                String url1 = annotation.value();

                Method[] methods = c.getMethods();

                for(Method m : methods) {
                    RestfulDispatcher dispatcher = null;
                    if(m.isAnnotationPresent(Get.class)) {
                        Get get = m.getAnnotation(Get.class);
                        dispatcher = new RestfulDispatcher(url1 + get.value(), controller, m, RestfulHttpMethod.GET, get.isPattern());
                    } else if(m.isAnnotationPresent(Post.class)) {
                        Post post = m.getAnnotation(Post.class);
                        dispatcher = new RestfulDispatcher(url1 + post.value(), controller, m, RestfulHttpMethod.POST, post.isPattern());
                    } else if(m.isAnnotationPresent(Put.class)) {
                        Put put = m.getAnnotation(Put.class);
                        dispatcher = new RestfulDispatcher(url1 + put.value(), controller, m, RestfulHttpMethod.PUT, put.isPattern());
                    } else if(m.isAnnotationPresent(Delete.class)) {
                        Delete delete = m.getAnnotation(Delete.class);
                        dispatcher = new RestfulDispatcher(url1 + delete.value(), controller, m, RestfulHttpMethod.DELETE, delete.isPattern());
                    } else if(m.isAnnotationPresent(Request.class)) {
                        Request map = m.getAnnotation(Request.class);
                        dispatcher = new RestfulDispatcher(url1 + map.value(), controller, m, RestfulHttpMethod.MAP, map.isPattern());
                    }

                    if(dispatcher != null) {
                        if(dispatcher.isPattern) {
                            dispatcherPatternList.add(dispatcher);
                        } else {
                            dispatcherAbsMap.put(dispatcher.dispatcherUrl, dispatcher);
                        }
                    }
                }

                log.trace("加载Controller {} => {}", url1, path);
            }
        }

        return dispatcherPatternList.size() + dispatcherPatternList.size();
    }

    /** 执行参数中 beans 的带有 Bean 注解的方法, 并将返回值缓存, 在 Controller 中使用 Autowired 注解时自动注入该缓存*/
    public int addBeans(Object... beans) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        ArrayList<BeanWrapTmp> tmpList = new ArrayList<>();

        for(Object beanParent : beans) {
            Class clazz = beanParent.getClass();
            Method[] methods = clazz.getDeclaredMethods();

            for(Method method : methods) {
                Bean b = method.getAnnotation(Bean.class);
                if(b != null) {
                    String name = b.value();
                    if(Utils.CheckNull(name)) {
                        name = clazz.getName();
                    }

                    BeanWrapTmp tmp = new BeanWrapTmp(beanParent, name, b.order(), method);
                    tmpList.add(tmp);
                }
            }
        }
        //根据order排序, 否则交叉引用时会出问题
        tmpList.sort(Comparator.comparingInt(BeanWrapTmp::getSort).reversed());

        for(BeanWrapTmp tmp : tmpList) {
            //Class beanType = tmp.method.getReturnType();

            Object beanObj = tmp.method.invoke(tmp.obj);
            beansCache.put(tmp.name, beanObj);
            RestfulUtils.injectBeansToObject(this, beanObj);
        }

        return tmpList.size();
    }



    //配置
    /** 处理请求业务线程池大小 */
    protected int eventThreadPoolSize = 20;

    /** request & response buffer初始大小 */
    protected int bufferSize = 1024 * 4;

    /** 最大允许的请求体大小 */
    protected int maxContentLength = 1024 * 1024 * 2;

    protected boolean isEnableGZIP = false;
    protected Charset charset = Charset.forName("utf-8");

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }


    public void setEnableGZIP(boolean enableGZIP) {
        isEnableGZIP = enableGZIP;
    }

    public void setEventThreadPoolSize(int eventThreadPoolSize) {
        this.eventThreadPoolSize = eventThreadPoolSize;
    }

    public void setFilter(RestfulFilter filter) {
        this.filter = filter;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public RestfulFilter getFilter() {
        return filter;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public boolean isEnableGZIP() {
        return isEnableGZIP;
    }

    public Charset getCharset() {
        return charset;
    }

    public Object getBean(String name) {
        return beansCache.get(name);
    }

    public void setOutModelAndViewTime(boolean outModelAndViewTime) {
        this.outModelAndViewTime = outModelAndViewTime;
    }

    protected TemplateEngine templateEngine = null;
    protected boolean outModelAndViewTime = true;

    /** 启用模板引擎,启用后支持处理 controller 返回的 ModelAndView */
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
        if(isRunning) {
            throw new Exception("启动后不支持再设置模板引擎");
        }
        if(templateEngine != null) {
            try {
                templateEngine.clearTemplateCache();
                templateEngine.clearDialects();
                templateEngine = null;
            } catch(Exception e) {
                log.warn("清理模板引擎出错:", e);
            }
        }
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

}
