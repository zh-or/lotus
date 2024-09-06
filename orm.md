## 注意目前只支持可mysql
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

## 其他方法可查看 `DatabaseExecutor` 类
