package io.jetproxy.middleware.cache;

import io.jetproxy.context.AppConfig;

import java.util.Map;
import java.util.stream.Collectors;

public class CacheFactory {

    public static String HTTP_REQUEST_CACHE_KEY = "http_request::%s_%s";
    public static String HTTP_FORWARD_AUTH_CACHE_KEY = "http_forward_auth::%s";
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

    public static String generateCacheKeyFromHeaders(String format, Map<String, String> headers) {
        String key = ":" + headers.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return String.format(format, key);
    }
}
