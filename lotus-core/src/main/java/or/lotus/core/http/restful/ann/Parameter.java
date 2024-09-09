package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target ({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    public static final String NULL_DEF_VALUE = "NULL";

    /** post json 的 path, get 的 name, 不填 name 参数是对象, 并且是post JSON则尝试直接解析json为对象 */
    String value() default "";
    String def() default NULL_DEF_VALUE;
}
