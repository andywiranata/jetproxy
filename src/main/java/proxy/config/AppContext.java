package proxy.config;

import proxy.cache.LRUCacheWithTTL;

import java.util.Map;

public class AppContext {
    private final AppConfig config;
    private final LRUCacheWithTTL cache;

    private AppContext(String configFilePath, Builder builder) throws Exception {
        this.config = ConfigLoader.getConfig(configFilePath);
        this.cache = new LRUCacheWithTTL(builder.maxSize, builder.maxHeapMemory);
    }

    public AppConfig getConfig() {
        return config;
    }

    public Map<String, String> getServiceMap() {
        return ConfigLoader.getServiceMap();
    }

    public LRUCacheWithTTL getCache() {
        return cache;
    }

    // Builder class
    public static class Builder {
        private int maxSize = 10000; // Default size
        private long maxHeapMemory = 50 * 1024 * 1024; // Default max heap memory (50 MB)

        public Builder withMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder withMaxHeapMemory(long maxHeapMemory) {
            this.maxHeapMemory = maxHeapMemory;
            return this;
        }

        public AppContext build(String configFilePath) throws Exception {
            return new AppContext(configFilePath, this);
        }
    }
}