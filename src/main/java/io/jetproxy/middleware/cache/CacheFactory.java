package io.jetproxy.middleware.cache;

import io.jetproxy.context.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CacheFactory {
    private static final Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    public static final String HTTP_REQUEST_CACHE_KEY = "http_request::%s_%s";
    public static final String HTTP_JWT_AUTH_SOURCE_CACHE_KEY = "http_jwt_auth_source::%s::%s";

    private static final int DEFAULT_SIZE = 10000;
    private static final long DEFAULT_MAX_MEMORY = 50 * 1024 * 1024; // Convert MB to bytes

    public static Cache createCache(AppConfig config) {
        if (config == null || config.getStorage() == null) {
            logger.warn("AppConfig or Storage is null, falling back to default in-memory cache.");
            return createDefaultInMemoryCache();
        }

        if (config.hasEnableRedisStorage()) {
            return initializeRedisCache(config);
        }

        if (config.hasEnableInMemoryStorage()) {
            return createInMemoryCache(config);
        }

        logger.info("No specific storage configuration found, using default in-memory cache.");
        return createDefaultInMemoryCache();
    }

    private static Cache initializeRedisCache(AppConfig config) {
        logger.info("Initializing Redis cache with configuration: {}", config.getStorage().getRedis());
        RedisPoolManager.initializePool(config.getStorage().getRedis());
        return new RedisCache();
    }

    private static Cache createInMemoryCache(AppConfig config) {
        AppConfig.Storage.InMemoryConfig inMemoryConfig = Optional.ofNullable(config.getStorage().getInMemory()).orElse(new AppConfig.Storage.InMemoryConfig());

        int size = Optional.ofNullable(inMemoryConfig.getSize()).orElse(DEFAULT_SIZE);
        long maxMemory = Optional.ofNullable(inMemoryConfig.getMaxMemory())
                .map(mb -> mb * 1024 * 1024)
                .orElse(DEFAULT_MAX_MEMORY);

        logger.info("Initializing In-Memory Cache with size={} and maxMemory={} bytes", size, maxMemory);
        return new LRUCacheWithTTL(size, maxMemory);
    }

    private static Cache createDefaultInMemoryCache() {
        logger.warn("Falling back to default in-memory cache with size={} and maxMemory={} bytes", DEFAULT_SIZE, DEFAULT_MAX_MEMORY);
        return new LRUCacheWithTTL(DEFAULT_SIZE, DEFAULT_MAX_MEMORY);
    }
}
