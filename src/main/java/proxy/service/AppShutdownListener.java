package proxy.service;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.middleware.cache.RedisPoolManager;

public class AppShutdownListener implements ServletContextListener {
    // Same as above
    private static final Logger logger = LoggerFactory.getLogger(AppShutdownListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        ServletContextListener.super.contextInitialized(sce);
        logger.info("AppShutdownListener context initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        RedisPoolManager.closePool();
        logger.info("Redis connection pool closed successfully.");
    }
}
