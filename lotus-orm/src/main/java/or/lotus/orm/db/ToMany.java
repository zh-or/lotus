package or.lotus.orm.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 连表查询 展开bean的字段
 * 注意: 使用此注解后, 统计分页总数可能会有错误需要另写sql查询总数
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToMany {
    /** 当前对象的主键, 如果此字段为空则获取 DataSourceConfig.getPrimaryKeyName() 的值  */
    String primaryKey() default "";

    /**参数应对应bean字段名*/
    String[] columns();

    /** 主键, 和columns的字段必须对应 */
    String[] primaryKeys();

    /**参数应对应数据库结果集字段名, 查询结果集的前缀, 比如查询结果字段名为 u_id 应传入 u_*/
    String[] prefix() default {};
}
