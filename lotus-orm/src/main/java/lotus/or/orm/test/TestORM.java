package lotus.or.orm.test;

import lotus.or.orm.db.Database;
import lotus.or.orm.pool.DataSourceConfig;
import lotus.or.orm.pool.LotusConnection;
import lotus.or.orm.pool.LotusDataSource;
import or.lotus.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class TestORM {
    static final Logger log = LoggerFactory.getLogger(TestORM.class);
    public static void main(String[] args) throws SQLException {
        DataSourceConfig config = new DataSourceConfig("jdbc:mysql://127.0.0.1:3306/test", "root", "123456");
        //DataSourceConfig config = new DataSourceConfig("jdbc:mysql://192.168.1.3:3306/test", "root", "b7ve4%7z^6");
        LotusDataSource dataSource = new LotusDataSource();
        dataSource.setConfig(config);
        printInfo(dataSource);
        testConnection(dataSource);
        testORM(dataSource);
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

    public static void testORM(LotusDataSource dataSource) {

        Database db = new Database();
        db.registerDataSource(dataSource);
        Test i = new Test();
        i.setStr("插入测试1");
        i.setCreateTime(new Date());

        int r = db.insert(i);
        int r2 = db.insert(i);

        i.setStr("修改");
        i.setCreateTime(null);

        int r3 = db.update(i);

        Test dto = db.selectDto(Test.class, "select * from test where id = ?").params(1).findOne();
        Test where = db.select(Test.class, 1);
        Test t = db.select(Test.class).findOne();
        Test t2 = db.select(Test.class).fields("str").findOne();
        long total = db.select(Test.class).findCount();

        Map<String, Object> map = db.select(Test.class).findOneMap();

        log.info("orm end");

    }


}
