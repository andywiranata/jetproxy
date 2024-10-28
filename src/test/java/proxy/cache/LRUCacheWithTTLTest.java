package proxy.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import proxy.middleware.cache.LRUCacheWithTTL;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheWithTTLTest {

    private LRUCacheWithTTL cache;
    private static final long MAX_HEAP_MEMORY = 1024 * 1024; // 1 MB for testing

    @BeforeEach
    void setUp() {
        cache = new LRUCacheWithTTL(3, MAX_HEAP_MEMORY);
    }

    @Test
    void testPutAndGet() {
        cache.put("key1", "value1", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);
        assertEquals("value1", cache.get("key1"), "Should return value1 for key1");

        cache.put("key2", "value2", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);
        assertEquals("value2", cache.get("key2"), "Should return value2 for key2");
    }

    @Test
    void testExpiredEntry() throws InterruptedException {
        cache.put("key1", "value1", 100); // 100ms TTL
        TimeUnit.MILLISECONDS.sleep(200); // Wait for TTL to expire

        assertNull(cache.get("key1"), "Expired entry should return null");
    }

    @Test
    void testMemoryLimitExceeded() {
        cache = new LRUCacheWithTTL(3, 50); // Simulating a small heap memory limit

        cache.put("key1", "val1", LRUCacheWithTTL.CacheEntry.NO_EXPIRED); // size ~ 10 bytes
        cache.put("key2", "val2", LRUCacheWithTTL.CacheEntry.NO_EXPIRED); // size ~ 10 bytes

        assertEquals("val1", cache.get("key1"), "Should return val1 for key1");
        assertEquals("val2", cache.get("key2"), "Should return val2 for key2");

        // Adding a new entry should trigger cleanup since the memory limit is exceeded
        cache.put("key3", "val3", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);
        assertNull(cache.get("key1"), "LRU entry should be evicted due to memory limit");
    }

    @Test
    void testEvictLeastRecentlyUsedEntry() {
        cache.put("key1", "value1", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);
        cache.put("key2", "value2", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);
        cache.put("key3", "value3", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);

        // Access key1 so that key2 becomes the least recently used
        cache.get("key1");

        // Add another entry, this should evict key2
        cache.put("key4", "value4", LRUCacheWithTTL.CacheEntry.NO_EXPIRED);

        assertNull(cache.get("key2"), "Least recently used entry (key2) should be evicted");
        assertEquals("value1", cache.get("key1"), "key1 should still be in the cache");
        assertEquals("value4", cache.get("key4"), "key4 should be in the cache");
    }
    
}
