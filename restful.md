## 注意事项
>1. `controller` 中的方法的参数不能为基本数据类型, 必须为包装类型, 否则会报错 ~~int~~, ~~long~~... Integer &#10004; Long&#10004;
>2. `controller` 支持两种返回值, `String`, `ModelAndView`, 其他返回值会直接调用 `toString` 并返回
>3.

## 使用需继承并实现 *3* 个类
1. 继承 `RestfulContext` 类, 用于接收请求并处理返回值
2. 继承 `RestfulRequest` 类, 用于包装`Request`可在这里面保存相关上下文
3. 继承 `RestfulResponse` 类, 用于处理 `controller` 结果返回

## 注解
1. `@Autowired` 用于`filter/bean/controller`中自动注入对象
2. `@Bean` 用于启动时注册bean, 该注解用于标注方法
3. `@RestfulController` 用于标注controller, `scanController`只会加载有该注解的类并解析
4. `@Request` 用于标注controller中的方法
5. `@Get` 用于标注controller中的方法
6. `@Post` 用于标注controller中的方法
7. `@Put` 用于标注controller中的方法
8. `@Delete` 用于标注controller中的方法
9. `@Parameter`  用于标注controller方法中的参数


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
