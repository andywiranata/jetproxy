package io.jetproxy.middleware.metric;

import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;

import java.util.ArrayList;
import java.util.List;

public class MetricsListenerFactory {
    private static final Logger logger = LoggerFactory.getLogger(MetricsListenerFactory.class);

    public static MetricsListener createMetricsListener(AppConfig config) {
        List<MetricsListener> listeners = new ArrayList<>();
        AppConfig.Storage.InMemoryConfig inMemoryConfig = config.getStorage().getInMemory();
        AppConfig.Storage.RedisConfig redisConfig = config.getStorage().getRedis();
        AppConfig.Storage.StatsdConfig statsdConfig = config.getStorage().getStatsd();

        if (inMemoryConfig.isEnabled()) {
            listeners.add(new InMemoryMetricsListener());
            logger.info("Metric In Memory Config Enabled");
        }

        if (redisConfig.isEnabled()) {
            // TODO redis implement
            listeners.add(new RedisMetricsListener());
            logger.info("Metric Redis Config Enabled");
        }

        if (statsdConfig.isEnabled()) {
            StatsDClient statsDClient = new NonBlockingStatsDClient(
                    statsdConfig.getPrefix(),
                    statsdConfig.getHost(),
                    statsdConfig.getPort());
            listeners.add(new StatsDMetricsListener(statsDClient));
            logger.info("Metric Statsd Enabled");
        }
        // Return a composite listener that wraps multiple listeners
        return new CompositeMetricsListener(listeners);
    }
}