package io.jetproxy.middleware.cache;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Cache {
    String get(String key);
    void put(String key, String value, long ttl);
    String getAsideStrategy(String key, long ttl, Supplier<String> fetchFunction);
}
