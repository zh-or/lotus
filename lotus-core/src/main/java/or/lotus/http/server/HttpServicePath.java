package or.lotus.http.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 优先全字匹配, 如果不存在则尝试正则匹配
 */
@Target ({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpServicePath {
    String value();

    boolean required() default true;

    String defaultValue() default "";

    /**是否正则表达式*/
    boolean reg() default false;
}
