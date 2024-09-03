package or.lotus.core.http.rest.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 返回Bean的方法
 */
@Target ({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    /**如果不填值则默认为类名如: com.a.b.User, 与 @Autowired 对应*/
    String value();
}
