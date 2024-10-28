package proxy.context;

import lombok.Getter;
import proxy.middleware.cache.Cache;
import proxy.middleware.cache.CacheFactory;
import proxy.middleware.cache.RedisPoolManager;
import proxy.middleware.metric.MetricsListener;
import proxy.middleware.metric.MetricsListenerFactory;

import java.util.Map;

@Getter
public class AppContext {
    // Volatile keyword ensures visibility of changes to variables across threads
    private static volatile AppContext instance;
    private final AppConfig config;
    private final Cache cache;
    private final MetricsListener metricsListener;
    private final boolean debugMode;

    private AppContext(Builder builder) {

        // Initialize instance with config, cache, and metrics listener
        this.config = ConfigLoader.getConfig(builder.pathConfigYaml);
        RedisPoolManager.initializePool(this.config.getStorage().getRedis());
        this.cache = CacheFactory.createCache(this.config);
        this.metricsListener = MetricsListenerFactory.createMetricsListener(this.config);
        this.debugMode = this.config.isDebugMode();
    }

    // Thread-safe singleton implementation using double-checked locking
    public static AppContext get() {
        if (instance == null) { // First check (no locking)
            synchronized (AppContext.class) { // Synchronize only on the first initialization
                if (instance == null) { // Second check (after acquiring lock)
                    instance = new Builder().build(); // Create the instance if not already created
                }
            }
        }
        return instance;
    }

    public Map<String, AppConfig.Service> getServiceMap() {
        return ConfigLoader.getServiceMap();
    }

    // Builder class
    public static class Builder {
        private String pathConfigYaml;

        public Builder withPathConfig(String pathConfigYaml) {
            if (pathConfigYaml == null || pathConfigYaml.isEmpty()) {
                this.pathConfigYaml = "config.yaml";
                return this;
            }
            this.pathConfigYaml = pathConfigYaml;
            return this;
        }

        public AppContext build() {
            return new AppContext(this);
        }
    }
}
