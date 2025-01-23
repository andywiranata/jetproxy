package io.jetproxy.middleware.log;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import io.jetproxy.context.AppConfig;
import org.slf4j.LoggerFactory;

public class LogbackConfigurator {
    public static void configureLogging(AppConfig.Logging loggingConfig) {
        if (loggingConfig == null || loggingConfig.getAppenders() == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        // Configure Root Logger
        Level rootLevel = Level.toLevel(loggingConfig.getRoot().getLevel(), Level.INFO);
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(rootLevel);

        // Configure Appenders
        for (AppConfig.Appender appenderConfig : loggingConfig.getAppenders()) {
            ConsoleAppender consoleAppender = new ConsoleAppender();
            consoleAppender.setName(appenderConfig.getName());
            consoleAppender.setContext(context);

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setPattern(appenderConfig.getEncoder().getPattern());
            encoder.setContext(context);
            encoder.start();

            consoleAppender.setEncoder(encoder);
            consoleAppender.start();

            rootLogger.addAppender(consoleAppender);
        }

        if (loggingConfig.getLoggers() != null) {
            // Configure Custom Loggers
            for ( AppConfig.Logger loggerConfig : loggingConfig.getLoggers()) {
                ch.qos.logback.classic.Logger logger = context.getLogger(loggerConfig.getName());
                Level level = Level.toLevel(loggerConfig.getLevel(), Level.DEBUG);
                logger.setLevel(level);
                logger.setAdditive(false); // Prevent logging from being duplicated in the root logger
            }
        }


    }
}
