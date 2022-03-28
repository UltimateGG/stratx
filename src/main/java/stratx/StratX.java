package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StratX {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("StratX");
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));

    static {
        if (!DEVELOPMENT_MODE) System.setProperty("env", "prod");
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void log(String msg, Object... args) {
        LOGGER.info(msg, args);
    }

    public static void warn(String msg, Object... args) {
        LOGGER.warn(msg, args);
    }

    public static void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
    }

    public static void trace(String msg, Object... args) {
        LOGGER.trace(msg, args);
    }
}
