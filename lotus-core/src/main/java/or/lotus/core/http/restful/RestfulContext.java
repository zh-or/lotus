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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
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
 * 2. 调用addBeans添加 bean, 如果bean继承了 AutoClose 接口, 那么停止 (stop()) 的时候会自动调用close方法
 * 3. 调用scanController扫描Controller类, 并实现@RestfulController注解
 * 4. http容器收到请求时调用dispatch方法分发请求, 返回值为需要输出的内容
 *  */
public abstract class RestfulContext {
    public static final String TAG = "Lotus Restful";
    protected static final Logger log = LoggerFactory.getLogger(RestfulContext.class);
    protected InetSocketAddress bindAddress;

    /**key = @Bean->value | packageName*/
    protected ConcurrentHashMap<String, Object> beansCache;

    /** key = url + http-method-str*/
    protected ConcurrentHashMap<String, RestfulDispatchMapper> dispatcherAbsMap;
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
        isRunning = true;
        onStart();
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
        for(Object b : beansCache.values()) {
            try {
                if(b.getClass().isAssignableFrom(AutoCloseable.class)) {
                    AutoCloseable closeable = (AutoCloseable) b;
                    closeable.close();
                }
            } catch (Exception e) {
                log.error("关闭bean失败:" + b, e);
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

                // 普通请求
                RestfulDispatchMapper mapper = getDispatcher(request);
                if(mapper != null) {
                    RestfulDispatcher dispatcher = mapper.getDispatcher(request.getMethod());

                    if(dispatcher == null) {
                        response.setStatus(RestfulResponseStatus.CLIENT_ERROR_METHOD_NOT_ALLOWED);
                        //response.clearWrite().write("");
                        sendResponse(true, request, response);
                        return ;
                    }

                    if(filter != null && filter.beforeRequest(dispatcher, request, response)) {
                        sendResponse(true, request, response);
                        return ;
                    }

                    Object ret = dispatcher.dispatch(this, request, response);
                    if(filter != null) {
                        if(filter.afterRequest(request, response, ret)) {
                            return;
                        }
                    }

                    if(ret == null) {

                    } else if(ret instanceof ModelAndView) {
                        if(templateEngine == null) {
                            throw new IllegalStateException("你返回了ModelAndView, 但是并没有启用模板引擎.");
                        }

                        handleModelAndView(request, response, (ModelAndView) ret);
                    } else if(ret instanceof File) {
                        response.write((File) ret);
                    } else {
                        response.write(ret.toString());
                    }

                    sendResponse(true, request, response);
                    return;
                }
                //未匹配, 未处理当前请求
                sendResponse(false, request, response);

            } catch (Throwable rawException) {
                response.clearWrite();
                response.setStatus(RestfulResponseStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
                Throwable e = rawException instanceof InvocationTargetException ? ((InvocationTargetException) rawException).getTargetException() : rawException;
                if(filter != null && filter.exception(e, request, response)) {
                    sendResponse(true, request, response);
                } else {

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

    protected void handleModelAndView(RestfulRequest request, RestfulResponse response, ModelAndView mv) throws IOException {
        if(mv.isRedirect) {//302跳转
            response.redirect(mv.getViewName());
        } else {
            try {
                templateEngine.process(
                        mv.getViewName(),
                        mv.values,
                        response
                );
            } catch(Exception e) {
                if (filter != null) {
                    response.clearWrite();
                    filter.exception(e, request, response);
                } else {
                    log.error("处理模板出错:", e);
                }
                return ;
            }

            if(outModelAndViewTime) {
                try {
                    response.write("<!-- handle time: " + ((System.nanoTime() - mv.createTime) / 1_000_000) + "ms -->");
                } catch (IOException e) {}
            }
            response.setHeader("Content-Type", "text/html; charset=" + response.charset.displayName());
        }
    }

    protected RestfulDispatchMapper getDispatcher(RestfulRequest request) {
        //todo 需要优化url匹配算法

        // 普通请求
        RestfulDispatchMapper mapper = dispatcherAbsMap.get(request.getUrl());
        if(mapper != null) {
            return mapper;
        }

        //正则url验证
        for(RestfulDispatcher dispatcher : dispatcherPatternList) {
            if(dispatcher.checkPattern(request)) {
                return new RestfulDispatchMapper(dispatcher);
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
            addController(c);
        }

        return dispatcherPatternList.size() + dispatcherPatternList.size();
    }

    public void addController(Class<?> c) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        RestfulController annotation = c.getAnnotation(RestfulController.class);
        if(annotation != null) {
            //controller 只创建一次
            Constructor constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object controller = constructor.newInstance();

            //注入bean
            RestfulUtils.injectBeansToObject(this, controller);

            //只加载有注解的类
            String url1 = annotation.value();

            Method[] methods = c.getMethods();

            for(Method method : methods) {
                RestfulDispatcher dispatcher = null;
                if(method.isAnnotationPresent(Get.class)) {
                    Get get = method.getAnnotation(Get.class);
                    dispatcher = new RestfulDispatcher(url1 + get.value(), controller, method, RestfulHttpMethod.GET, get.isPattern());
                } else if(method.isAnnotationPresent(Post.class)) {
                    Post post = method.getAnnotation(Post.class);
                    dispatcher = new RestfulDispatcher(url1 + post.value(), controller, method, RestfulHttpMethod.POST, post.isPattern());
                } else if(method.isAnnotationPresent(Put.class)) {
                    Put put = method.getAnnotation(Put.class);
                    dispatcher = new RestfulDispatcher(url1 + put.value(), controller, method, RestfulHttpMethod.PUT, put.isPattern());
                } else if(method.isAnnotationPresent(Delete.class)) {
                    Delete delete = method.getAnnotation(Delete.class);
                    dispatcher = new RestfulDispatcher(url1 + delete.value(), controller, method, RestfulHttpMethod.DELETE, delete.isPattern());
                } else if(method.isAnnotationPresent(Request.class)) {
                    Request map = method.getAnnotation(Request.class);
                    dispatcher = new RestfulDispatcher(url1 + map.value(), controller, method, RestfulHttpMethod.REQUEST, map.isPattern());
                }

                if(dispatcher != null) {

                    if(dispatcher.isPattern) {
                        dispatcherPatternList.add(dispatcher);
                    } else {
                        RestfulDispatchMapper old = dispatcherAbsMap.get(dispatcher.url);
                        if(old == null) {
                            old = new RestfulDispatchMapper();
                            dispatcherAbsMap.put(dispatcher.url, old);
                        }
                        old.setDispatcher(dispatcher);
                    }
                }
            }

            log.trace("加载Controller {} => {}", url1, c.getName());
        }
    }

    /** 手动注册Bean */
    public RestfulContext addBean(Object bean) throws IllegalAccessException {
        Utils.assets(bean == null, "bean 不能为空");
        return addBean(null, bean);
    }

    /** 手动注册Bean */
    public RestfulContext addBean(String name, Object bean) throws IllegalAccessException {
        RestfulUtils.injectBeansToObject(this, bean);
        if(Utils.CheckNull(name)) {
            name = bean.getClass().getName();
        }
        beansCache.put(name, bean);
        return this;
    }

    /** 1. 扫描指定包名
     * 2. 实例化所有带有Bean注解的类
     * 3. 注入Bean
     * 4. 添加该类到Bean缓存
     * 注意: 该方法只适合没有构造参数的类, 否则会报错
     * */
    public List<String> addBeansFromPackage(String packageName) throws Exception {
        Utils.assets(packageName, "包名不能为空");
        List<String> clazzs = BeanUtils.getClassPathByPackage(packageName);
        ArrayList<BeanSortWrap> tmpList = new ArrayList<>();
        for (String path : clazzs) {
            Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(path);
            Bean b = c.getAnnotation(Bean.class);
            if(b != null) {
                Constructor constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                String name = b.value();
                if(Utils.CheckNull(name)) {
                    name = c.getName();
                }
                tmpList.add(new BeanSortWrap(constructor, name, b.order(), null));
            }
        }
        //根据order排序, 否则交叉引用时会出问题
        tmpList.sort(Comparator.comparingInt(BeanSortWrap::getSort).reversed());

        for(BeanSortWrap tmp : tmpList) {
            Constructor constructor = (Constructor) tmp.obj;
            Object obj = constructor.newInstance();
            addBean(tmp.name, obj);
        }

        return clazzs;
    }

    /** 执行参数中 beans 的带有 Bean 注解的方法, 并将返回值缓存, 在 Controller 中使用 Autowired 注解时自动注入该缓存 */
    public List<String> addBeansFromMethodReturn(Object... beans) throws InvocationTargetException, IllegalAccessException {
        ArrayList<BeanSortWrap> tmpList = new ArrayList<>();
        List<String> addedBeans = new ArrayList<>();
        for(Object beanParent : beans) {
            Class clazz = beanParent.getClass();
            Method[] methods = clazz.getDeclaredMethods();

            for(Method method : methods) {
                Bean b = method.getAnnotation(Bean.class);
                if(b != null) {
                    String name = b.value();
                    if(Utils.CheckNull(name)) {
                        //获取返回值的全限定名
                        name = method.getReturnType().getName();
                    }
                    addedBeans.add(name);
                    BeanSortWrap tmp = new BeanSortWrap(beanParent, name, b.order(), method);
                    tmpList.add(tmp);
                }
            }
        }
        //根据order排序, 否则交叉引用时会出问题
        tmpList.sort(Comparator.comparingInt(BeanSortWrap::getSort).reversed());

        for(BeanSortWrap tmp : tmpList) {
            //先注入bean, 再执行方法
            RestfulUtils.injectBeansToObject(this, tmp.obj);

            Object beanObj = tmp.method.invoke(tmp.obj);
            beansCache.put(tmp.name, beanObj);
            RestfulUtils.injectBeansToObject(this, beanObj);
        }

        return addedBeans;
    }



    //配置
    /** 处理请求业务线程池大小 */
    protected int eventThreadPoolSize = 20;

    /** request & response buffer初始大小 */
    protected int bufferSize = 1024 * 4 * 2;

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

    /** 会在filter内注入bean */
    public void setFilter(RestfulFilter filter) {
        this.filter = filter;
        try {
            RestfulUtils.injectBeansToObject(this, filter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    public <T> T getBean(Class<T> clazz) {
        return (T) beansCache.get(clazz.getName());
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
