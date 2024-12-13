package io.jetproxy.middleware.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPoolManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisPoolManager.class);

    private static JedisPool jedisPool = null;

    private RedisPoolManager() {}

    // Method to initialize the Redis pool with configuration
    public static void initializePool(AppConfig.Storage.RedisConfig config) {
        if (jedisPool == null) {
            synchronized (RedisPoolManager.class) {
                if (jedisPool == null) {
                    // Configure your pool settings here
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    poolConfig.setMaxTotal(config.getMaxTotal());
                    poolConfig.setMaxIdle(config.getMaxIdle());
                    poolConfig.setMinIdle(config.getMinIdle());

                    // Initialize the Redis pool with the config and Redis server address
                    jedisPool = new JedisPool(poolConfig, config.getHost(), config.getPort());
                    logger.info("Redis connection pool initialized.");
                }
            }
        }
    }

    // Method to get the existing Redis pool (assuming it's already initialized)
    public static JedisPool getPool() {
        if (jedisPool == null) {
            logger.error("Redis pool not initialized. Call initializePool first.");
            throw new IllegalStateException("Redis pool not initialized. Call initializePool first.");
        }
        return jedisPool;
    }

    // Method to close the Redis pool
    public static void closePool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            logger.info("Redis connection pool closed");
            jedisPool.close();
        }
    }
}
