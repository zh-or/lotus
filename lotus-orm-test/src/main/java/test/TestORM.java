package test;

import lotus.or.orm.db.Database;
import lotus.or.orm.db.DatabaseExecutor;
import lotus.or.orm.db.JdbcUtils;
import lotus.or.orm.db.Transaction;
import lotus.or.orm.pool.DataSourceConfig;
import lotus.or.orm.pool.LotusConnection;
import lotus.or.orm.pool.LotusDataSource;
import or.lotus.common.Format;
import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestORM {
    static final Logger log = LoggerFactory.getLogger(TestORM.class);
    public static void main(String[] args) throws Exception {
        DataSourceConfig config = new DataSourceConfig("jdbc:mysql://127.0.0.1:3306/test", "root", "123456");
        //DataSourceConfig config = new DataSourceConfig("jdbc:mysql://192.168.1.3:3306/test", "root", "");
        LotusDataSource dataSource = new LotusDataSource();
        dataSource.setConfig(config);
        printInfo(dataSource);
        testConnection(dataSource);
        testORM3(dataSource);
        //testORM(dataSource);
    }

    public static void printInfo(LotusDataSource dataSource) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    log.info("dataSource: {}", dataSource.toString());
                    Utils.SLEEP(1000);
                }
            }
        }).start();
    }


    public static void testConnection(LotusDataSource dataSource) throws SQLException {

        ArrayList<LotusConnection> arr = new ArrayList<>();

        for(int i = 0; i < 11; i ++) {
            LotusConnection conn = (LotusConnection) dataSource.getConnection();
            arr.add(conn);
        }
        Utils.SLEEP(1000);

        for(LotusConnection conn : arr) {
            try(PreparedStatement ps = conn.prepareStatement("select count(id) from test");
                ResultSet rs = ps.executeQuery();) {
                if(rs.next()) {
                    log.info("连接: {}, 返回: {}", conn.getId(), rs.getInt(1));
                } else {
                    log.warn("未查询到任何东西");
                }
            }

            conn.close();
        }

        log.info("end:{}", dataSource.toString());
    }

    public static void testORM(LotusDataSource dataSource) throws SQLException {

        Database db = new Database();
        db.registerDataSource(dataSource);
        Test i = new Test();
        i.setStr("插入测试1");
        i.setCreateTime(new Date());

        int r = db.insert(i);

        List<Object> list = new ArrayList<>();
        list.add(i);
        list.add(i);

        int r2 = db.insertAll(list);
        Test i2 = new Test();
        i2.setStr("修改");
        i2.setCreateTime(null);
        i2.setId(i.getId());

        int u1 = db.update(i2);

        ((Test)list.get(0)).setStr("修改2");
        ((Test)list.get(1)).setStr("修改3");

        int u2 = db.updateAll(list);

        Test dto = db.selectDto(Test.class, "select * from test where id = ?").params(1).findOne();
        Test where = db.select(Test.class, 1);
        Test t = db.select(Test.class).findOne();
        Test t2 = db.select(Test.class).fields("str").findOne();
        long total = db.select(Test.class).findCount();

        Map<String, Object> map = db.select(Test.class).findOneMap();
        List<Map<String, Object>> maps = db.select(Test.class).whereLt("id", 10).findListMap();

        int del = db.delete(Test.class, t.getId());

        log.info("orm end");

    }

    public static void testORM2(LotusDataSource dataSource) throws SQLException {
        Database db = new Database();
        db.registerDataSource(dataSource);


        try(Transaction transaction = db.beginTransaction()) {
            Test t1 = new Test(0, "事务测试1", new Date());
            int r1 = db.insert(t1);
            int r2 = db.insert(new Test(0, "事务测试1", new Date()));

            transaction.commit();
        }

        List<Test> res = db.select(Test.class).whereLt("id", 4).whereAnd().whereGt("id", 2).findList();
        List<Test> res2 = db.select(Test.class).whereGt("id", 4).desc("id").findList();

        List<Test> res3 = db.select(Test.class).whereIn("id", 1, 2, 3).findList();
        List<Test> res4 = db.select(Test.class).whereNot("id", 4).findList();

        for(Test t : res2) {
            t.setStr("多更新:"  + Format.formatTime(System.currentTimeMillis()));
        }

        int mu1 = db.updateAll(res2);

        Test i = new Test();
        i.setStr("单独更新:" + Format.formatTime(System.currentTimeMillis()));
        i.setCreateTime(new Date());
        i.setId(2);
        int u1 = db.update(i);



        System.out.println("orm end");
    }

    public static void testORM3(LotusDataSource dataSource) throws SQLException, IOException {
        Database db = new Database();
        db.registerDataSource(dataSource);

        int r = db.execSqlUpdate("update test set type_id = ?").params(1).execute();
        DatabaseExecutor exec = db.selectDto(TestDto.class, JdbcUtils.sqlFromResources("test.sql"));
        long count = exec.findCount("id", "test");
        List<TestDto> list = exec.findList();

        System.out.println("orm end");
    }
}
