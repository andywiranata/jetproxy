package proxy.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCacheWithTTL {
    private static final Logger logger = LoggerFactory.getLogger(LRUCacheWithTTL.class);

    private final long maxHeapMemory; // Maximum heap memory in bytes
    private final LinkedHashMap<String, CacheEntry> cache;
    private final ReentrantLock lock = new ReentrantLock();
    private long currentMemoryUsage; // Track current memory usage

    // Cache entry with value and timestamp
    private static class CacheEntry {
        public static final long NO_EXPIRED = -1;
        String value;
        long timestamp;
        long ttl;

        CacheEntry(String value, long ttl) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
        }

        boolean isExpired() {
            if (ttl == NO_EXPIRED) {
                return false;
            }
            return (System.currentTimeMillis() - timestamp) > ttl;
        }

        long getSize() {
            return (value != null ? value.length() * Character.BYTES : 0) + Long.BYTES * 2; // size of String, timestamp, and ttl
        }
    }

    public LRUCacheWithTTL(int maxSize, long maxHeapMemory) {
        this.maxHeapMemory = maxHeapMemory;
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxSize || eldest.getValue().isExpired();
            }
        };
        this.currentMemoryUsage = 0; // Initialize memory usage
    }

    public String get(String key) {
        lock.lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                if (entry != null) {
                    currentMemoryUsage -= entry.getSize();
                }
                cache.remove(key); // Remove expired entry
                return null;
            }
            return entry.value;
        } finally {
            lock.unlock();
        }
    }

    public void put(String key, String value, long ttl) {
        lock.lock();
        try {
            CacheEntry newEntry = new CacheEntry(value, ttl);
            long newEntrySize = newEntry.getSize();

            // Check memory usage before adding the new entry
            while (isMemoryExceeded(newEntrySize)) {
                logger.warn("Memory limit exceeded. Performing cleanup.");
                cleanup(); // Clean up to free memory if limit exceeded
            }
            logger.info("new key {} {} {}", key, value, ttl);
            cache.put(key, newEntry);
            currentMemoryUsage += newEntrySize;
        } finally {
            lock.unlock();
        }
    }

    private boolean isMemoryExceeded(long additionalSize) {
        return (currentMemoryUsage + additionalSize) > maxHeapMemory;
    }

    private void cleanup() {
        // Remove the least recently used entries until memory is below the threshold
        while (isMemoryExceeded(0) && !cache.isEmpty()) {
            String eldestKey = cache.entrySet().iterator().next().getKey();
            logger.info("Removing entry {} due to memory cleanup", eldestKey);
            CacheEntry eldestEntry = cache.remove(eldestKey);
            currentMemoryUsage -= eldestEntry.getSize();
        }
    }
}
