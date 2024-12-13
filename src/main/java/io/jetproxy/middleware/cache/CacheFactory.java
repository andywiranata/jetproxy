package io.jetproxy.middleware.cache;

import io.jetproxy.context.AppConfig;

public class CacheFactory {
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
