package or.lotus.db;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;

public class MapperWrap implements AutoCloseable {
    protected GoSqlSession _sqlSession = null;
    protected DB db;
    protected int getMapperCount = 0;

    public MapperWrap(DB db) {
        this.db = db;
    }

    private void checkSession() {
        if (_sqlSession == null) {
            _sqlSession = db.getSqlSession();
        }
    }

    /**切换时会导致之前的getMapper失效
     * ExecutorType.SIMPLE：该类型的执行器没有特别的行为。它为每个语句的执行创建一个新的预处理语句。 无论autoCommit为true或false都会返回id
     * ExecutorType.REUSE：该类型的执行器会复用预处理语句。批量插入时使用, autoCommit应为false
     * ExecutorType.BATCH：该类型的执行器会批量执行所有更新语句，批量更新时使用, autoCommit应为false
     * */
    public void toggleMode(ExecutorType type, boolean autoCommit) throws SQLException {
        if(_sqlSession != null) {
            _sqlSession.close();
        }
        if(getMapperCount > 0) {
            throw new SQLException("由于已经获取了mapper, 此时再切换模式会导致此前回去的mapper失效");
        }
        _sqlSession = db.getSqlSession(type, autoCommit);
    }

    public void clearMapperCount() {
        getMapperCount = 0;
    }

    public <T> T getMapper(Class<T> clazz) {
        getMapperCount ++;
        checkSession();
        return _sqlSession.getMapper(clazz);
    }

    public void commit() {
        checkSession();
        _sqlSession.commit();
    }

    public SqlSession getSqlSession() {
        checkSession();
        return _sqlSession;
    }

    /**
     * 默认使用 ExecutorType.SIMPLE, 此种方法在执行插入时数据库id会自增, 也会返回id
     * */
    public void beginJdbcTransaction() throws SQLException {
        beginJdbcTransaction(ExecutorType.SIMPLE);
    }

    /**
     * ExecutorType.BATCH 此种方法在执行插入时不会返回id, 数据库id也不会自增
     * ExecutorType.SIMPLE, 此种方法在执行插入时数据库id会自增, 也会返回id
     * */
    public void beginJdbcTransaction(ExecutorType type) throws SQLException {
        toggleMode(type, false);
    }

    public void commitJdbcTransaction() {
        _sqlSession.commit();
    }

    public void rollbackJdbcTransaction() {
        _sqlSession.rollback();
    }

    @Override
    public void close()  {
        if(_sqlSession != null) {
            _sqlSession.close();
        }
    }
}
