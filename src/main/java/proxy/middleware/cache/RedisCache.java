package proxy.cache;
import proxy.context.AppConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(String.valueOf(key), ttl / 1000, value);
        }
    }
}