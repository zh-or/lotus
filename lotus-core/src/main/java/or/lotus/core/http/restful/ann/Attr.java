package or.lotus.core.http.restful.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** 自动读取 request 的 attribute */
@Target ({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Attr {
    /** request 的 attribute key */
    String value();
}
