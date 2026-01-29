## 实现
>lotus-core -> HttpServer 基于nio的实现
>lotus-netty-http -> NettyHttpServer 基于netty的实现

## 注意事项
>1. `controller` 中的方法的参数不能为基本数据类型, 必须为包装类型, 否则会报错 ~~int~~, ~~long~~... Integer &#10004; Long&#10004;
>2. `controller` 支持3种返回值, `File`, `String`, `ModelAndView`, 其他返回值会直接调用 `toString` 并返回


## 注解
1. `@Autowired` 
   - `filter/bean/controller`中成员变量自动注入`bean`, `prop`
   - `controller` 的参数注入`bean`, `prop`
2. `@Bean` 用于启动时注册bean, 该注解用于标注对象
   - `context.addBeansFromMethodReturn` 添加的类的方法标注`@Bean`
3. `@Attr` 用于 `controller` 的参数注入`request.attribute`
4. `@Prop`
   - `filter/bean/controller`中成员变量自动注入`context.property`
   - `controller` 的参数注入`context.property`
5. `@Parameter`  用于标注controller方法中的参数
6. `@PathVar` url路径参数
7. `@RestfulController` 用于标注controller, `scanController`只会加载有该注解的类并解析
8. `@Request` 用于标注controller中的方法
9. `@Get` 用于标注controller中的方法
10. `@Post` 用于标注controller中的方法
11. `@Put` 用于标注controller中的方法
12. `@Delete` 用于标注controller中的方法

## url匹配
1. `/api/hello` 匹配 `/api/hello`
2. `/api/{id}` 匹配 `/api/[任意字符]` 可通过`@PathVar("id")` 注解取值
3. `/api/*` 匹配 `/api/[任意字符]`  不可通过`@PathVar("id")` 注解取值


## 使用

*controller*
```java
@RestfulController("/api")
public class ControllerA {
    
    @Autowired
    UserService userService;
    
    @Get("/hello")
    public String hello(@Paramter("name") String name) {
        return "hello:" + name;
    }
}
```

>`controller`返回值支持 `File`,`String`, `ModelAndView`, `Object` <br>
> 其中 `Object` 会自动调用`toString`方法<br>
> `File` 会根据文件类型自动设置`Content-Type`<br>
> `ModelAndView` 需要在context中启用模板引擎, 并设置模板引擎路径

*controller* 中的方法除了使用`@Paramter`外, `RestfulContext`, `RestfulRequest`, `RestfulResponse` 不需要注解直接自动注入


## `@Paramter` 使用说明
1. `GET` | `POST (urlencoded)` | `DELETE` | `OPTIONS`
   * `@Paramter("key")` => 基本数据类型, [], List<基本数据类型>
2. `POST (json)`
   * `@Paramter` => 对象, List<对象>, List<基本数据类型>
   * `@Paramter("key")` => 基本数据类型, 对象, List<对象>, List<基本数据类型>

## `@Attr` 使用说明
1. `@Attr("key")` => 获取当前`request`的 `attribute`, 可在`filter`中设置`attr`

## 处理文件上传说明
1. `POST (multipart/form-data)`
2. `controller` 方法中使用 `RestfulFormData formData = request.getBodyFormData();`
3. `RestfulFormData` 可获取 `formData` 的相关数据
4. `request.isMultipart()` 可判断当前请求是否 `multipart` 请求



## 如需使用其他http容器需要继承并实现 *3* 个类
1. 继承 `RestfulContext` 类, 用于接收请求并处理返回值
2. 继承 `RestfulRequest` 类, 用于包装`Request`可在这里面保存相关上下文
3. 继承 `RestfulResponse` 类, 用于处理 `controller` 结果返回
4. 如需处理`POST (multipart/form-data)`请求还需要实现`RestfulFormData`相关类
