package or.lotus.orm.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * todo 当bean的字段使用此注解时, 自动查询一次关联表, 并且将结果赋值到bean中
 * */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToMany {
    /**主键*/
    String oneField();
    /**关联主键*/
    String manyField();
}
