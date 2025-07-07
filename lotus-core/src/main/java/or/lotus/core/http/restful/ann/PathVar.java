package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** 自动读取 request 的 path中的参数 */
@Target ({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVar {
    /** url 路径中的key 如 {name}  */
    String value();
}
