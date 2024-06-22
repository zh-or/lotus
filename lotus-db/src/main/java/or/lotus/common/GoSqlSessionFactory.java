package or.lotus.common;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

import java.sql.SQLException;

public class GoSqlSessionFactory extends DefaultSqlSessionFactory {

    public GoSqlSessionFactory(SqlSessionFactory factory) {
        this(factory.getConfiguration());
    }

    public GoSqlSessionFactory(Configuration configuration) {
        super(configuration);
    }

    public GoSqlSession openGoSqlSession(boolean autoCommit) {
        if(autoCommit) {
            return openGoSqlSession(ExecutorType.SIMPLE, autoCommit);
        }
        return openGoSqlSession(ExecutorType.BATCH, autoCommit);
    }

    public GoSqlSession openGoSqlSession(ExecutorType execType, boolean autoCommit) {
        Transaction tx = null;
        try {
            Configuration configuration = getConfiguration();
            final Environment environment = configuration.getEnvironment();
            final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
            tx = transactionFactory.newTransaction(environment.getDataSource(), null, autoCommit);
            final Executor executor = configuration.newExecutor(tx, execType);
            return new GoSqlSession(configuration, executor, autoCommit);
        } catch (Exception e) {
            closeTransaction(tx); // may have fetched a connection so lets call close()
            throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
        if (environment == null || environment.getTransactionFactory() == null) {
            return new ManagedTransactionFactory();
        }
        return environment.getTransactionFactory();
    }

    private void closeTransaction(Transaction tx) {
        if (tx != null) {
            try {
                tx.close();
            } catch (SQLException ignore) {
                // Intentionally ignore. Prefer previous error.
            }
        }
    }
}
