package io.jetproxy.middleware.cache;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class RedisPoolManager {
    public static String CHANNEL_CONFIG_CHANGE = "jetproxy_config_change";
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
    // Redis Publisher
    public static void publish(String channel, String message) {
        try (Jedis jedis = getPool().getResource()) {
            jedis.publish(channel, message);
            logger.info("Published message to channel {}: {}", channel, message);
        } catch (Exception e) {
            logger.error("Failed to publish message to channel {}: {}", channel, e.getMessage(), e);
        }
    }

    // Redis Subscriber
    public static void subscribe(String channel, JedisPubSub listener) {
        new Thread(() -> {
            try (Jedis jedis = getPool().getResource()) {
                logger.info("Subscribing to channel: {}", channel);
                jedis.subscribe(listener, channel);
                logger.info("Subscription to channel {} ended", channel);
            } catch (Exception e) {
                logger.error("Error during subscription to channel {}: {}", channel, e.getMessage(), e);
            }
        }).start();
    }
}
