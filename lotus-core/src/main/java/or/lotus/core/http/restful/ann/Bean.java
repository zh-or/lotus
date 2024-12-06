package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标注bean的类
 */
@Target ({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    /**如果不填值则默认为类名如: com.a.b.User, 与 @Autowired 对应*/
    String value() default "";

    /** 交叉引用时需要明确哪个 bean 先加载, 数值大的优先加载 */
    int order() default 0;
}
