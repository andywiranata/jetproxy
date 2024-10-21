package proxy.cache;
import proxy.context.AppConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCache implements Cache {
    private final JedisPool jedisPool;

    public RedisCache(AppConfig config) {
        AppConfig.Storage.RedisConfig redisConfig = config.getStorage().getRedis();
        this.jedisPool = new JedisPool(
                redisConfig.getHost(),
                redisConfig.getPort());
    }

    @Override
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(String.valueOf(key));
        }
    }

    @Override
    public void put(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(String.valueOf(key), -1, value);
        }
    }
}