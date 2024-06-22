package or.lotus.common;

import com.fasterxml.jackson.databind.module.SimpleModule;
import or.lotus.geometry.model.LineStringGeo;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;
import java.io.InputStream;

public class DB {
    GoSqlSessionFactory sqlSessionFactory;


    public DB(String myBatisConfigPath) throws Exception {
        InputStream inputStream = new FileInputStream(myBatisConfigPath);
        sqlSessionFactory = new GoSqlSessionFactory(new SqlSessionFactoryBuilder().build(inputStream));
        //打开然后关闭测试连接是否正常
        sqlSessionFactory.openSession().close();
    }

    public void addGeoModel() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LineStringGeo.class, new LineStringGeoJsonSerializer());
        module.addDeserializer(LineStringGeo.class, new LineStringGeoJsonDeserializer());
        BeanUtils.OBJECT_MAPPER.registerModule(module);
    }

    /**
     * 输出为DEBUG等级
     */
    public void enableOutLog() {
        sqlSessionFactory.getConfiguration().setLogImpl(Slf4jImpl.class);
    }

    public GoSqlSession getSqlSession() {
        return getSqlSession(true);
    }

    public GoSqlSession getSqlSession(boolean autoCommit) {
        return sqlSessionFactory.openGoSqlSession(autoCommit);
    }

    public GoSqlSession getSqlSession(ExecutorType execType, boolean autoCommit) {
        return sqlSessionFactory.openGoSqlSession(execType, autoCommit);
    }


}
