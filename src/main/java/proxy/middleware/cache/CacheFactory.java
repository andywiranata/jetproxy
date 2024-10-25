package proxy.middleware.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;

public class CacheFactory {
    private static final Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    public static Cache createCache(AppConfig config) {
        AppConfig.Storage.InMemoryConfig inMemoryConfig = config.getStorage().getInMemory();
        AppConfig.Storage.RedisConfig redisConfig = config.getStorage().getRedis();

        if (redisConfig.isEnabled()) {
            return new RedisCache();
        }

        return new LRUCacheWithTTL(
                    inMemoryConfig.getSize(),
                    inMemoryConfig.getMaxMemory() * 1024 * 1024);

    }
}
