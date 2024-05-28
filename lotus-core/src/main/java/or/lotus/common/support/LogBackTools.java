package or.lotus.common.support;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.slf4j.LoggerFactory;

public class LogBackTools {

    /**
     * 手动加载logback日志配置文件
     * @param path
     * @throws Exception
     */
    public static void loadConfig(String path) throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(path);
        //StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }
}
