package or.lotus.mybatis.common;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

public class DruidTransactionFactory implements DataSourceFactory {

    private DruidDataSource dataSource;

    @Override
    public void setProperties(Properties props) {
        props.setProperty("driverClassName", props.getProperty("driver"));
        try {
            dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(props);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("we cant config druid");
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
}
