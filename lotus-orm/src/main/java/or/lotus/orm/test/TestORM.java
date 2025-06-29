package or.lotus.orm.test;

import or.lotus.core.common.DateUtils;
import or.lotus.orm.db.*;
import or.lotus.orm.geometry.GeometryConvertToModel;
import or.lotus.orm.geometry.model.PointGeo;
import or.lotus.orm.pool.DataSourceConfig;
import or.lotus.orm.pool.LotusConnection;
import or.lotus.orm.pool.LotusDataSource;
import or.lotus.core.common.Utils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
        testSqlRes();
    }

    public static void testSqlRes() {
    }

    public static void testDs() throws Exception {
        DataSourceConfig config = new DataSourceConfig("jdbc:mysql://127.0.0.1:3306/test", "root", "123456");
        //DataSourceConfig config = new DataSourceConfig("jdbc:mysql://192.168.1.3:3306/test", "root", "");

        config.addTypeConvert(PointGeo.class, new TypeConvert<PointGeo>() {

            @Override
            public String sqlParam() {
                return "ST_PointFromWKB(?, 4326)";
            }

            @Override
            public PointGeo decode(ResultSet ps, String columnName) throws SQLException {

                InputStream in = ps.getBinaryStream(columnName);
                return GeometryConvertToModel.toPointGeo(in);
            }

            @Override
            public void encode(PreparedStatement ps, int index, PointGeo obj) throws SQLException{
                Geometry geo = obj.toGeometry();
                //geo.setSRID(4326);
                int dimension = geo.getDimension();
                dimension = (dimension == 2 || dimension == 3) ? dimension : 2;
                //有SRID的话前面会多4个字节, 但是插入数据库不需要这一节, 需要在sql中指定SRID的值
                //http://www.tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/io/WKBWriter.html
                //文档
                WKBWriter write = new WKBWriter(dimension, false);
                byte[] data = write.write(geo);
                ps.setBytes(index, data);
            }
        });

        LotusDataSource dataSource = new LotusDataSource();
        dataSource.setConfig(config);
        //printInfo(dataSource);
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
            t.setStr("多更新:"  + DateUtils.format(System.currentTimeMillis()));
        }

        int mu1 = db.updateAll(res2);

        Test i = new Test();
        i.setStr("单独更新:" + DateUtils.format(System.currentTimeMillis()));
        i.setCreateTime(new Date());
        i.setId(2);
        int u1 = db.update(i);



        System.out.println("orm end");
    }

    public static void testORM3(LotusDataSource dataSource) throws SQLException, IOException {
        Database db = new Database();
        db.registerDataSource(dataSource);

        int rId = Utils.RandomNum(2, 18);
        Test ut = new Test();
        ut.setId(rId);
        ut.setP(new PointGeo(106.53, 29.5306));

        int updateR = db.update(ut);
        ut.setStr("插入带point的数据");
        int insertR = db.insert(ut, "create_time");

        List<Test> t1 = db.select(Test.class).findList();

        int r = db.execSqlUpdate("update test set type_id = ?").params(1).execute();
        DatabaseExecutor exec = db.selectDto(TestDto.class, JdbcUtils.sqlFromResources("test.sql"));
        long count = exec.findCount("id", "test");
        List<TestDto> list = exec.findList();

        System.out.println("orm end");
    }
}
