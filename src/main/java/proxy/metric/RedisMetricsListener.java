package proxy.metric;

import redis.clients.jedis.Jedis;

public class RedisMetricsListener implements MetricsListener {

    private final Jedis jedis;

    public RedisMetricsListener(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public void onProxyPathUsed(String path, long timestamp) {
        jedis.zadd("proxy:path:" + path, timestamp, String.valueOf(timestamp));
    }

    @Override
    public void onHttpStatusReturned(int statusCode, long timestamp) {
        jedis.zadd("http:status:" + statusCode, timestamp, String.valueOf(timestamp));
    }

    @Override
    public void onResponseSizeRecorded(String path, long size, long timestamp) {
        jedis.zadd("response:size:" + path, timestamp, String.valueOf(size));
    }
}