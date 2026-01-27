package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target ({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    public static final String DEF_NULL_VALUE = "Parameter-NULL";

    /** post json时如果以"/"开头则调用at方法支持层级 /person/name, 否则调用path方法, get 的 name, 不填 name 参数则直接转换为对象 */
    String value() default "";
    String def() default DEF_NULL_VALUE;
}
