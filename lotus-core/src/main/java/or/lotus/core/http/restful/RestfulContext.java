package or.lotus.core.http.restful;

import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.ann.*;
import or.lotus.core.http.restful.support.BeanWrapTmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 2. 调用addBeans添加 bean
 * 3. 调用scanController扫描Controller类, 并实现@RestfulController注解
 * 4. http容器收到请求时调用dispatch方法分发请求, 返回值为需要输出的内容
 *  */
public abstract class RestfulContext {
    static final Logger log = LoggerFactory.getLogger(RestfulContext.class);
    protected InetSocketAddress bindAddress;
    protected static final Charset charset = Charset.forName("utf-8");

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

    public void start(int port) {
        start(new InetSocketAddress(port));
    }

    public void start(String host, int port) {
        start(new InetSocketAddress(host, port));
    }

    public synchronized void start(InetSocketAddress bindAddress) {
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

    protected synchronized void stop() {
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

    protected abstract void onStart();
    protected abstract void onStop();

    /** 分发请求 */
    protected RestfulResponse dispatch(RestfulRequest request) {
        if(filter != null) {
            RestfulResponse response = filter.beforeRequest(request);
            if(response != null) {
                return response;
            }
        }
        // 普通请求
        RestfulDispatcher dispatcher = dispatcherAbsMap.get(request.getDispatchUrl());
        if(dispatcher != null) {
            try {
                RestfulResponse response = dispatcher.dispatch(this, request);
                RestfulResponse response2 = filter.afterRequest(request, response);
                if(response2 != null) {
                    return response2;
                }
                return response;
            } catch (Throwable e) {
                if(filter != null) {
                    filter.exception(e, request, response);
                }
            }
        }
        //正则url验证
        for(RestfulDispatcher patternDispatcher : dispatcherPatternList) {
            if(patternDispatcher.checkPattern(request)) {
                try {
                    RestfulResponse response = patternDispatcher.dispatch(this, request);
                    RestfulResponse response2 = filter.afterRequest(request, response);
                    if(response2 != null) {
                        return response2;
                    }
                    return response;
                } catch (Throwable e) {

                }
            }
        }

        //文件请求
        String url = request.getUrl();

        //todo 分发请求
        return null;
    }

    protected void callException(Throwable throwable) {

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
                injectBeansToObject(controller);

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
            Class beanType = tmp.method.getReturnType();
            if(beanType != null && beanType != void.class) {
                Object beanObj = beanType.getDeclaredConstructor().newInstance();
                beansCache.put(tmp.name, beanObj);
                injectBeansToObject(beanObj);
            }
        }

        return tmpList.size();
    }


    /** 注入Bean到对象 */
    public void injectBeansToObject(Object controllerObject) throws IllegalAccessException {
        Class clazz = controllerObject.getClass();
        Field[] fields = clazz.getFields();

        for(Field field : fields) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if(autowired != null) {
                String name = autowired.value();
                if(Utils.CheckNull(name)) {
                    //使用全限定名
                    name = field.getDeclaringClass().getName();
                }

                Object bean = beansCache.get(name);
                if(bean != null) {
                    field.setAccessible(true);
                    field.set(controllerObject, bean);
                } else {
                    throw new RuntimeException(String.format(
                            "%s 注入 %s 时 %s 还未注册, 请检查是否在 addBeans 方法之前调用了 scanController ",
                            clazz.getName(),
                            name,
                            name
                    ));
                }
            }
        }
    }

    //配置
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

    public void setFilter(RestfulFilter filter) {
        this.filter = filter;
    }
}
