package proxy.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppContext;

public class DebugAwareLogger {

    private final Logger logger;
    private final boolean debugMode;

    private DebugAwareLogger(Class<?> clazz, boolean debugMode) {

        this.logger = LoggerFactory.getLogger(clazz);
        this.debugMode = debugMode;
    }

    public static DebugAwareLogger getLogger(Class<?> clazz) {
        return new DebugAwareLogger(clazz, AppContext.get().isDebugMode());
    }

    public void debug(String message, Object... args) {
        if (debugMode) {
            logger.info(message, args);
        } else {
            logger.debug(message, args);
        }
    }

    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    // Additional logging methods as needed
}
