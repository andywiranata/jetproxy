package proxy.metric;

import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import proxy.config.AppConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Jedis;
import java.util.ArrayList;
import java.util.List;

public class MetricsListenerFactory {

    public static MetricsListener createMetricsListener() {
        List<MetricsListener> listeners = new ArrayList<>();

        String metricsBackends = System.getenv("METRICS_BACKENDS"); // comma-separated value: "inmemory,redis"

        if (metricsBackends != null) {
            String[] backends = metricsBackends.split(",");
            for (String backend : backends) {
                switch (backend.trim().toLowerCase()) {
                    case "redis":
                        String redisHost = System.getenv("REDIS_HOST");
                        int redisPort = Integer.parseInt(System.getenv("REDIS_PORT"));
                        Jedis jedis = new Jedis(redisHost, redisPort);
                        listeners.add(new RedisMetricsListener(jedis));
                        break;
                    case "statsd":
                        // Initialize StatsD client
                        String statsdHost = System.getenv("STATSD_HOST");
                        int statsdPort = Integer.parseInt(System.getenv("STATSD_PORT"));
                        StatsDClient statsDClient = new NonBlockingStatsDClient("my.prefix", statsdHost, statsdPort);
                        listeners.add(new StatsDMetricsListener(statsDClient));
                        break;
                    case "inmemory":
                        listeners.add(new InMemoryMetricsListener());
                        break;
                }
            }
        } else {
            // Default to in-memory if no valid backend is set
            listeners.add(new InMemoryMetricsListener());
        }

        // Return a composite listener that wraps multiple listeners
        return new CompositeMetricsListener(listeners);
    }
}
//
//public class MetricsListenerFactory {
//
//    public static MetricsListener createMetricsListener(AppConfig config) {
//        String metricsBackend = System.getenv("METRICS_BACKEND");
//
//        switch (metricsBackend != null ? metricsBackend.toLowerCase() : "") {
//            case "redis":
//                // Initialize Redis client
//                String redisHost = System.getenv("REDIS_HOST");
//                int redisPort = Integer.parseInt(System.getenv("REDIS_PORT"));
//                Jedis jedis = new Jedis(redisHost, redisPort);
//                return new RedisMetricsListener(jedis);
//
//            case "statsd":
//                // Initialize StatsD client
//                String statsdHost = System.getenv("STATSD_HOST");
//                int statsdPort = Integer.parseInt(System.getenv("STATSD_PORT"));
//                StatsDClient statsDClient = new NonBlockingStatsDClient("my.prefix", statsdHost, statsdPort);
//                return new StatsDMetricsListener(statsDClient);
//
//            case "in-memory":
//            default:
//                // Default to in-memory if no valid backend is set
//                return new InMemoryMetricsListener();
//        }
//    }
//}
