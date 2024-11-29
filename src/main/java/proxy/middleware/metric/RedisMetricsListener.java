package proxy.middleware.metric;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import proxy.middleware.cache.RedisPoolManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class RedisMetricsListener implements MetricsListener {
    private final JedisPool jedisPool;
    private static final DateTimeFormatter DATE_HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

    public RedisMetricsListener() {
        this.jedisPool = RedisPoolManager.getPool();
    }

    @Override
    public void captureMetricProxyResponse(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        String queryParams = request.getQueryString();
        String fullPath = queryParams == null ? path : path + "?" + queryParams;
        int statusCode = response.getStatus();

        // Generate Redis key with path and current hour timestamp
        String currentHourKey = DATE_HOUR_FORMATTER.format(LocalDateTime.now());
        String redisKey = "metrics:" + fullPath + ":" + currentHourKey;

        // Store metrics in Redis
        try (Jedis jedis = jedisPool.getResource()) {
            // Increment the hit count and status code count in Redis
            jedis.hincrBy(redisKey, "hitCount", 1);
            jedis.hincrBy(redisKey, "status:" + statusCode, 1);

            // log.info("Captured response for {} -> Status: {}, Redis Key: {}", fullPath, statusCode, redisKey);
        }
    }

    // Retrieve metrics for a specific path over the last 24 hours
    public void getMetricsForPathLast24Hours(String path) {
        LocalDateTime now = LocalDateTime.now();
        try (Jedis jedis = jedisPool.getResource()) {
            for (int i = 0; i < 24; i++) {
                String hourlyKey = "metrics:" + path + ":" + DATE_HOUR_FORMATTER.format(now.minusHours(i));
                // log.info("Metrics for {}: {}", hourlyKey, jedis.hgetAll(hourlyKey));
            }
        }
    }
}
