package proxy.context;

import proxy.cache.LRUCacheWithTTL;
import proxy.cache.RedisCache;
import proxy.metric.MetricsListener;
import proxy.metric.MetricsListenerFactory;

import java.util.Map;

public class AppContext {
    private static AppContext instance;
    private final AppConfig config;
    private final LRUCacheWithTTL cache;
    private final RedisCache redisCache;
    private final MetricsListener metricsListener;

    private AppContext(Builder builder)  {
        // initialize instance
        this.config = ConfigLoader.getConfig(builder.pathConfigYaml);
        this.cache = new LRUCacheWithTTL(builder.maxSize, builder.maxHeapMemory);
        this.redisCache = new RedisCache(this.config);
        this.metricsListener = MetricsListenerFactory.createMetricsListener(this.config);
    }

    public static AppContext getInstance() {
        if (instance == null) {
            instance = new Builder().build(); // Create default instance if not already created
        }
        return instance;
    }

    public MetricsListener getMetricsListener() {
        return metricsListener;
    }

    public AppConfig getConfig() {
        return config;
    }

    public Map<String, AppConfig.Service> getServiceMap() {
        return ConfigLoader.getServiceMap();
    }

    public LRUCacheWithTTL getCache() {
        return cache;
    }

    // Builder class
    public static class Builder {
        private int maxSize = 10000; // Default size
        private long maxHeapMemory = 50 * 1024 * 1024; // Default max heap memory (50 MB)
        private String pathConfigYaml;
        public Builder withMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder withMaxHeapMemory(long maxHeapMemory) {
            this.maxHeapMemory = maxHeapMemory;
            return this;
        }
        public Builder withPathConfig(String pathConfigYaml) {
            if (pathConfigYaml.isEmpty()) {
                this.pathConfigYaml = "config.yaml";
                return this;
            }
            this.pathConfigYaml = pathConfigYaml;
            return this;
        }


        public AppContext build()  {
            return new AppContext(this);
        }
    }
}