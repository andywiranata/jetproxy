package io.jetproxy.service;

import io.jetproxy.context.AppContext;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.cache.RedisPoolManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppShutdownListener implements ServletContextListener {
    // Same as above
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(AppShutdownListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (AppContext.get().isGracefullyShutdownAllowed()) {
            RedisPoolManager.closePool();
            logger.info("Shutting down gracefully...");
        }

    }
}
