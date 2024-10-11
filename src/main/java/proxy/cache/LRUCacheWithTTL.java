package proxy.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCacheWithTTL implements Cache {
    private final int maxSize;
    private final long ttl; // Time-To-Live in milliseconds
    private final LinkedHashMap<Integer, CacheEntry> cache;
    private final ReentrantLock lock = new ReentrantLock();

    // Cache entry with value and timestamp
    private static class CacheEntry {
        String value;
        long timestamp;

        CacheEntry(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public LRUCacheWithTTL(int maxSize, long ttl) {
        this.maxSize = maxSize;
        this.ttl = ttl;
        this.cache = new LinkedHashMap<Integer, CacheEntry>(maxSize, 0.40f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, CacheEntry> eldest) {
                return size() > maxSize || isExpired(eldest.getValue());
            }
        };
    }

    private boolean isExpired(CacheEntry entry) {
        return (System.currentTimeMillis() - entry.timestamp) > ttl;
    }

    @Override
    public String get(int key) {
        lock.lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || isExpired(entry)) {
                cache.remove(key); // Remove expired entry
                return null;
            }
            return entry.value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(int key, String value) {
        lock.lock();
        try {
            cache.put(key, new CacheEntry(value));
        } finally {
            lock.unlock();
        }
    }
}