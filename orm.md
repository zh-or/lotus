## 注意目前只测试mysql

## 约定
1. 所有表的主键统一, 名称设置方法`dataSourceConfig.setPrimaryKeyName("id");`
2. 默认表名为类名 User -> user, UserOrder -> user_order, 如果不对应则使用 `@Table` 注解注明
3. 一对多查询&一对一查询 使用注解 `@ToMany` 
4. `@ToMany` 的 `prefix` 对应的实体类的字段不要添加该前缀

## `@ToMany` 注解使用说明 实体类对应注解与sql, 表: user, attachments, tweet

1. @ToMany -> columns 对应类的属性名称
2. @ToMany -> primaryKeys 对应主键名称, 该字段用于解析查询结果集时处理一对多一对一关系
3. @ToMany -> prefix 一对多或一对一的字段的前缀, 实体类的字段不要有该前缀, 会自动处理 

```java
@ToMany(columns = {"attachments", "user"}, primaryKeys = {"atth_id", "user_id"}, prefix = {"atth_", "u_"})
class Tweet {
    String content;
    
    User user;
    
    List<Atth> attachments;
    getter... 
    setter...
}
```

```sql

SELECT
a.*,
b.id as atth_id,
b.atth_data as atth_atth_data,
u.id as u_id,
u.nickname as u_nickname,

FROM tweet AS a 
LEFT JOIN attachments AS b ON b.tweet_id = a.id
LEFT JOIN `user` as u ON u.id = a.user_id

```

## 使用方法
1. 创建配置
```java
DataSourceConfig dataSourceConfig = new DataSourceConfig();
dataSourceConfig.setUsername(getStringConfig("db.username", ""));
dataSourceConfig.setPassword(getStringConfig("db.password", ""));
dataSourceConfig.setUrl(getStringConfig("db.url", ""));
dataSourceConfig.setMaxConnection(2);
dataSourceConfig.setMinConnection(1);
dataSourceConfig.setHeartbeatTimeoutSeconds(1);//心跳sql执行超时时间
dataSourceConfig.setHeartbeatFreqSecs(30);//心跳间隔
dataSourceConfig.setPrimaryKeyName("id");//主键, 这个比较重要后面更新删除等都会使用该值
dataSourceConfig.setDriverName("com.mysql.cj.jdbc.Driver");
dataSourceConfig.setPrintStackPackagePrefix("com.ls");//日志输出过滤, 只输出该包下的日志
dataSourceConfig.addTypeConvert(PointGeo.class, new PointGeoTypeConvert());//自定义类型转换, 当PO中的类型为 PointGeo 时 调用 PointGeoTypeConvert
dataSourceConfig.setPrintSqlLog(false);
```
2. 创建`DataSource`
```java
LotusDataSource dataSource = new LotusDataSource();
dataSource.setConfig(dataSourceConfig);
```
3. 创建执行器 `DataBase`
```java
Database db = new Database();
db.registerDataSource(dataSource);
//db.registerDataSource("nameForDataSource", dataSource);//传入名字后使用时可以通过名字获取数据源

```
4. 测试`Model`
```java
public class User {
    private Integer id;

    private String name;
    
    getter...
    setter...
}
```
4. 增删改查

插入
```java
User user = new User();
user.setName("xxx");
db.insert(user);
```
删除
```java
db.delete(User.class , 1);
```
修改
```java
//方式1
User user = new User();
user.setId(1);
user.setName("xxx");
db.update(user);
```
查询
```java
//方式1

db.select(User.class , 1);
db.select(User.class).whereEq("id", 1).findList();
db.select(User.class).whereEq("id", 1).findMap();
db.select(User.class).whereEq("id", 1).findOne();
db.select(User.class).whereEq("id", 1).findPage(1, 10);

db.selectDto(User.class, "select a.xx, b.cc from xx");
db.selectList(User.class, "select * from xx");
```

> 如果sql很长可使用`JdbcUtils.sqlFromResources("file.sql?marker")`读取`resources`文件夹下的文件 `#marker` 后面的sql<br>
> 路径后面的问号表示标记<br>
> 文件支持`-- 注释`

file.sql
```sql
#marker
--#号开始表示标记开始 两个减号开始表示注释开始, 注释到换行符结束
select * from user where id = ?
```



## 其他方法可查看 `DatabaseExecutor` 类
