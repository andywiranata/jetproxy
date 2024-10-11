package proxy.cache;
import redis.clients.jedis.Jedis;

public class RedisCache implements Cache {
    private final Jedis jedis; // Assuming you're using the Jedis library
    private final long ttl; // Time-To-Live in seconds

    public RedisCache(String redisHost, int redisPort, long ttl) {
        this.jedis = new Jedis(redisHost, redisPort);
        this.ttl = ttl;
    }

    @Override
    public String get(int key) {
        // Retrieve the value from Redis
        return jedis.get(String.valueOf(key));
    }

    @Override
    public void put(int key, String value) {
        // Store the value in Redis with TTL
        jedis.setex(String.valueOf(key), (int) ttl, value);
    }
}