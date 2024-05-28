package or.lotus.annotation;

import or.lotus.config.C;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
    /**
     * 所需权限数组
     */
    String[] value();

    int type() default C.AuthType.IN;
}
