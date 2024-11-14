package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入变量, 此注解可在 @Bean 返回的类和 @Controller 注解的类中使用
 */
@Target ({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
    /**如果不填值则默认为类名如: com.a.b.User, 与 @Bean 对应*/
    String value() default "";
}
