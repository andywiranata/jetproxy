package proxy.cache;

public interface RedisConfigStrategy {
    String getConfigValue(String key);
    void close();
}
