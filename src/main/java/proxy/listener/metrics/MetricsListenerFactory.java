package proxy.listener.metrics;

import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import redis.clients.jedis.Jedis;

public class MetricsListenerFactory {

    public static MetricsListener createMetricsListener() {
        String metricsBackend = System.getenv("METRICS_BACKEND");

        switch (metricsBackend != null ? metricsBackend.toLowerCase() : "") {
            case "redis":
                // Initialize Redis client
                String redisHost = System.getenv("REDIS_HOST");
                int redisPort = Integer.parseInt(System.getenv("REDIS_PORT"));
                Jedis jedis = new Jedis(redisHost, redisPort);
                return new RedisMetricsListener(jedis);

            case "statsd":
                // Initialize StatsD client
                String statsdHost = System.getenv("STATSD_HOST");
                int statsdPort = Integer.parseInt(System.getenv("STATSD_PORT"));
                StatsDClient statsDClient = new NonBlockingStatsDClient("my.prefix", statsdHost, statsdPort);
                return new StatsDMetricsListener(statsDClient);

            case "in-memory":
            default:
                // Default to in-memory if no valid backend is set
                return new InMemoryMetricsListener();
        }
    }
}
