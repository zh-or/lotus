package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target ({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {
    /** path */
    String value();
    boolean required() default true;

    String defaultValue() default "";

    /**是否正则表达式*/
    boolean isPattern() default false;
}
