package or.lotus.core.common;

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


    /**
     * 手动加载logback日志配置文件
     * @param resources 需要在最前面加 /
     * @throws Exception
     */
    public static void loadConfigFromResources(String resources) throws Exception {
        //好像会自动在这些目录查找配置文件, 不用手动调用
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(Thread.currentThread().getClass().getResourceAsStream(resources));
        //StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }
}
