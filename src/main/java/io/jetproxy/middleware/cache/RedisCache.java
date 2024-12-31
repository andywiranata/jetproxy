package io.jetproxy.middleware.cache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.Supplier;

public class RedisCache implements Cache {
    private final JedisPool jedisPool;

    public RedisCache() {
        this.jedisPool = RedisPoolManager.getPool();
    }

    @Override
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(String.valueOf(key));
        }
    }

    @Override
    public void put(String key, String value, long ttl) {
        if (ttl <= 0) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(String.valueOf(key), ttl / 1000, value);
        }
    }

    @Override
    public String getAsideStrategy(String key, long ttl, Supplier<String> fetchFunction) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Attempt to get the value from Redis
            String cacheData = jedis.get(key);

            if (cacheData != null) {
                // Return the cached value if it exists
                return cacheData;
            }

            // Fetch the data using the provided fetch function
            String fetchedData = fetchFunction.get();

            if (fetchedData != null) {
                // Store the fetched data in Redis with the specified TTL
                jedis.setex(key, (int) (ttl / 1000), fetchedData);
            }

            return fetchedData;
        }
    }


}