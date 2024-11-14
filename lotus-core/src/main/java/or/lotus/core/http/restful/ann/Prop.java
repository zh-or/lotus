package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 返回prop的内容
 */
@Target ({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Prop {
    String value() default "";//key
    String def() default "";//默认值

    /**从逗号隔开的值中随机取一个*/
    boolean random() default false;
}
