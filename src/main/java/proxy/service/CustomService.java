package proxy.service;

import proxy.cache.RedisConfigStrategy;

public class CustomService {
    private final RedisConfigStrategy redisConfigStrategy;

    public CustomService(RedisConfigStrategy redisConfigStrategy) {
        this.redisConfigStrategy = redisConfigStrategy;
    }

    public void executeLogic(String requestPath) {
        String redisKey = requestPath.substring(1);
        String value = redisConfigStrategy.getConfigValue(redisKey);

        if (value != null) {
            System.out.println("Logic executed for path: " + requestPath + " with value: " + value);
        } else {
            System.out.println("No value found in Redis for path: " + requestPath);
        }
    }
}
