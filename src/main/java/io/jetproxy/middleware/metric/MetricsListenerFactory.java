package io.jetproxy.middleware.metric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class MetricsListenerFactory {
    private static final Logger logger = LoggerFactory.getLogger(MetricsListenerFactory.class);

    public static MetricsListener createMetricsListener(AppConfig config) {
        List<MetricsListener> listeners = new ArrayList<>();
        AppConfig.Storage.InMemoryConfig inMemoryConfig = config.getStorage().getInMemory();
        AppConfig.Storage.RedisConfig redisConfig = config.getStorage().getRedis();

        if (inMemoryConfig.isEnabled()) {
            listeners.add(new InMemoryMetricsListener());
            logger.info("Metric In Memory Config Enabled");
        }

        if (redisConfig.isEnabled()) {
            // TODO redis implement
            listeners.add(new RedisMetricsListener());
            logger.info("Metric Redis Config Enabled");
        }

        return new CompositeMetricsListener(listeners);
    }
}