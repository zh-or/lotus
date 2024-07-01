package lotus.or.orm.db;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    ConcurrentHashMap<String, DataSource>  dataSources;

    public Database() {
        dataSources = new ConcurrentHashMap<String, DataSource>();
    }

    /**name for default*/
    public void registerDataSource(DataSource dataSource) {
        registerDataSource("default", dataSource);
    }
    public void registerDataSource(String name, DataSource dataSource) {
        dataSources.put(name, dataSource);
    }

}
