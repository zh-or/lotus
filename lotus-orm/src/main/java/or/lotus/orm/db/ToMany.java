package or.lotus.orm.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 连表查询 一对多, 一对一
 * 注意: 使用此注解后, 统计分页总数可能会有错误需要另写sql查询总数
 * 注意: 主表记录需要排在前面
 * 注意: 使用该注解时, 不要调用findOne() 方法来获取结果, 会导致最后只取一条数据
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToMany {
    /** 当前对象的主键, 如果此字段为空则获取 DataSourceConfig.getPrimaryKeyName() 的值  */
    String primaryKey() default "";

    /**参数应对应bean字段名*/
    String[] columns();

    /** 主键, 和columns的字段必须对应, 如果从结果集中未读取到主键的值则视为未查询到数据 */
    String[] primaryKeys();

    /**参数应对应数据库结果集字段名, 查询结果集的前缀, 比如查询结果字段名为 u_id 应传入 u_*/
    String[] prefix() default {};
}
