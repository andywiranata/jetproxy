package io.jetproxy.middleware.resilience;

import io.jetproxy.middleware.resilience.ratelimiter.RateLimiter;
import io.jetproxy.middleware.resilience.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    private RateLimiterConfig config;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(100)) // Refresh every 100ms
                .limitForPeriod(5)                         // 5 tokens per period
                .maxBurstCapacity(10)                      // Allow up to 10 tokens
                .build();
        rateLimiter = new RateLimiter(config);
    }

    @Test
    void testAllowRequestsWithinBurstCapacity() {
        // Allow up to burst capacity (10 tokens)
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryConsume(), "Request should be allowed within burst capacity");
        }

        // Further requests should be denied until tokens are refilled
        assertFalse(rateLimiter.tryConsume(), "Request should be denied after burst capacity is exceeded");
    }

    @Test
    void testSteadyTraffic() throws InterruptedException {
        // Consume initial burst (10 tokens)
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryConsume(), "Request should be allowed within burst capacity");
        }

        // Wait for 200ms (2 refill periods) to accumulate 10 tokens
        Thread.sleep(200);
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryConsume(), "Request should be allowed after tokens are refilled");
        }

        // Further requests should be denied
        assertFalse(rateLimiter.tryConsume(), "Request should be denied when tokens are exhausted");
    }

    @Test
    void testOverloadedTraffic() throws InterruptedException {
        // Send 20 requests immediately (exceeding burst + refill)
        int allowed = 0, denied = 0;

        for (int i = 0; i < 20; i++) {
            if (rateLimiter.tryConsume()) {
                allowed++;
            } else {
                denied++;
            }
        }

        // Check that only burst capacity (10 tokens) was allowed
        assertEquals(10, allowed, "Only burst capacity requests should be allowed");
        assertEquals(10, denied, "Requests beyond burst capacity should be denied");

        // Wait for one refill period and retry
        Thread.sleep(100);
        assertTrue(rateLimiter.tryConsume(), "Request should be allowed after tokens are refilled");
    }

    @Test
    void testRefillBehavior() throws InterruptedException {
        // Consume all tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryConsume(), "Request should be allowed within burst capacity");
        }

        // Wait for one refill period (5 tokens)
        Thread.sleep(100);
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsume(), "Request should be allowed after tokens are refilled");
        }

        // Further requests should be denied
        assertFalse(rateLimiter.tryConsume(), "Request should be denied when tokens are exhausted");

        // Wait for another refill period
        Thread.sleep(100);
        assertTrue(rateLimiter.tryConsume(), "Request should be allowed after additional tokens are refilled");
    }


}
