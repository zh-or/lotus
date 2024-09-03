package or.lotus.core.http.rest.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入变量
 */
@Target ({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
    /**如果不填值则默认为类名如: com.a.b.User, 与 @Bean 对应*/
    String value();
}
