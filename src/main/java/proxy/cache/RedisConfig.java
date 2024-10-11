package proxy.cache;

import redis.clients.jedis.Jedis;

public class RedisConfig implements RedisConfigStrategy {
    private Jedis jedis;

    public RedisConfig(String host, int port) {
        jedis = new Jedis(host, port);
    }

    @Override
    public String getConfigValue(String key) {
        return jedis.get(key);
    }

    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
