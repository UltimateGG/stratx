package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StratX {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("StratX");

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void log(String msg, Object... args) {
        LOGGER.info(String.format(msg, args));
    }

    public static void warn(String msg, Object... args) {
        LOGGER.warn(String.format(msg, args));
    }

    public static void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
    }

    public static void error(String msg, Object... args) {
        LOGGER.error(String.format(msg, args));
    }
}
