package io.jetproxy.middleware.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.jetproxy.middleware.resilience.retry.Retry;
import io.jetproxy.middleware.resilience.retry.RetryConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RetryTest {

    private Retry retry;

    @BeforeEach
    public void setup() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3) // Maximum 3 retries
                .waitDuration(Duration.ofMillis(100)) // 100ms delay between retries
                .build();
        retry = new Retry("retry-name", config);
    }

    @Test
    public void testCanRetryWithinMaxAttempts() {
        // Retry should allow up to 3 attempts
        assertTrue(retry.canRetry(), "First attempt should be allowed");
        assertTrue(retry.canRetry(), "Second attempt should be allowed");
        assertTrue(retry.canRetry(), "Third attempt should be allowed");
        assertFalse(retry.canRetry(), "Fourth attempt should not be allowed");
    }

    @Test
    public void testStateAfterExhaustingRetries() {
        // Exhaust retries
        retry.canRetry(); // Attempt 1
        retry.canRetry(); // Attempt 2
        retry.canRetry(); // Attempt 3
        retry.canRetry(); // Attempt 4 (should fail)

        assertEquals("FAILED", retry.getState(), "State should be FAILED after exhausting retries");
    }

    @Test
    public void testStateAfterSuccess() {
        // Simulate success after some retries
        retry.canRetry(); // Attempt 1
        retry.canRetry(); // Attempt 2
        retry.onSuccess(0, TimeUnit.MILLISECONDS); // Mark as success

        assertEquals("SUCCESS", retry.getState(), "State should be SUCCESS after successful operation");
        assertTrue(retry.canRetry(), "Retry mechanism should reset after success");
    }

    @Test
    public void testDelayBetweenRetries() {
        long startTime = System.currentTimeMillis();

        // Perform a retry with delay
        retry.onError(0, TimeUnit.MILLISECONDS);

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Assert that the delay is at least 100ms
        assertTrue(elapsedTime >= 100, "Retry should delay for at least 100ms before next attempt");
    }

    @Test
    public void testResetAfterSuccess() {
        // Perform some retries
        retry.canRetry(); // Attempt 1
        retry.canRetry(); // Attempt 2

        // Reset on success
        retry.onSuccess(0, TimeUnit.MILLISECONDS);

        // Retry should reset and allow up to 3 attempts again
        assertTrue(retry.canRetry(), "First attempt after reset should be allowed");
        assertTrue(retry.canRetry(), "Second attempt after reset should be allowed");
        assertTrue(retry.canRetry(), "Third attempt after reset should be allowed");
        assertFalse(retry.canRetry(), "Fourth attempt after reset should not be allowed");
    }
}
