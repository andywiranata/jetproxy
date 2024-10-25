package proxy.middleware.metric;

import redis.clients.jedis.Jedis;

public class RedisMetricsListener implements MetricsListener {

    private final Jedis jedis;

    public RedisMetricsListener(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public void onProxyPathUsed(String path, int statusCode, long size) {
        /*
        jedis.zadd("proxy:path:" + path, timestamp, String.valueOf(timestamp));
        jedis.zadd("http:status:" + statusCode, timestamp, String.valueOf(timestamp));
        jedis.zadd("response:size:" + path, timestamp, String.valueOf(size));
        */
    }
}