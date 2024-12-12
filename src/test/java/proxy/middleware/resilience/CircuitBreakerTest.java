package proxy.middleware.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import proxy.middleware.resilience.circuitbreaker.CircuitBreaker;
import proxy.middleware.resilience.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CircuitBreakerTest {

    private CircuitBreakerConfig config;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    public void setup() {
        config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofMillis(500))
                .waitDurationInOpenState(Duration.ofMillis(2000))
                .permittedNumberOfCallsInHalfOpenState(5)
                .minimumNumberOfCalls(10)
                .build();
        circuitBreaker = new CircuitBreaker("circuitbreaker",config);
    }

    @Test
    public void testInitialStateIsClosed() {
        assertEquals("CLOSED", circuitBreaker.getState(), "Initial state should be CLOSED");
    }

    @Test
    public void testAllowRequestInClosedState() {
        assertTrue(circuitBreaker.allowRequest(), "Request should be allowed in CLOSED state");
    }

    @Test
    public void testTransitionToOpenStateDueToFailures() {
        // Simulate 10 failed requests
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        }

        assertEquals("OPEN", circuitBreaker.getState(), "Circuit breaker should transition to OPEN state");
    }

    @Test
    public void testTransitionToOpenStateDueToSlowCalls() {
        // Simulate 10 slow calls
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onSuccess(600, TimeUnit.MILLISECONDS); // Slow call > 500ms
        }

        assertEquals("OPEN", circuitBreaker.getState(), "Circuit breaker should transition to OPEN state due to slow calls");
    }

    @Test
    public void testHalfOpenStateAfterWaitDuration() throws InterruptedException {
        // Transition to OPEN state
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        }

        assertEquals("OPEN", circuitBreaker.getState(), "Circuit breaker should be in OPEN state");

        // Wait for the configured wait duration
        Thread.sleep(2000);

        assertTrue(circuitBreaker.allowRequest(), "Request should be allowed after wait duration in OPEN state");
        assertEquals("HALF_OPEN", circuitBreaker.getState(), "Circuit breaker should transition to HALF_OPEN state");
    }

    @Test
    public void testTransitionToClosedStateFromHalfOpen() throws InterruptedException {
        // Transition to OPEN state
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        }

        // Wait for the configured wait duration
        Thread.sleep(2000);
        assertTrue(circuitBreaker.allowRequest(), "Request should be allowed after wait duration in OPEN state");

        // Simulate successful calls in HALF_OPEN state
        for (int i = 0; i < 5; i++) { // Configured permittedNumberOfCallsInHalfOpenState
            circuitBreaker.onSuccess(200, TimeUnit.MILLISECONDS);
        }

        assertEquals("CLOSED", circuitBreaker.getState(), "Circuit breaker should transition back to CLOSED state");
    }


    @Test
    public void testTransitionBackToOpenFromHalfOpen() throws InterruptedException {
        // Transition to OPEN state
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        }

        // Wait for the configured wait duration
        Thread.sleep(2000);
        assertTrue(circuitBreaker.allowRequest(), "Request should be allowed after wait duration in OPEN state");

        // Simulate a failed call in HALF_OPEN state
        circuitBreaker.onError(100, TimeUnit.MILLISECONDS);

        assertEquals("OPEN", circuitBreaker.getState(), "Circuit breaker should transition back to OPEN state from HALF_OPEN");
    }

    @Test
    public void testAllowRequestInHalfOpenState() throws InterruptedException {
        // Transition to OPEN state
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        }

        // Wait for the configured wait duration
        Thread.sleep(2000);

        // Allow the first 5 requests in HALF_OPEN state
        for (int i = 0; i < 6; i++) {
            assertTrue(circuitBreaker.allowRequest(), "Request should be allowed in HALF_OPEN state");
        }

        // Reject the 6th request
        assertFalse(circuitBreaker.allowRequest(), "Request should be rejected after reaching limit in HALF_OPEN state");

    }

    @Test
    public void testClosedToHalfOpenToOpenToClosed() throws InterruptedException {
        // Simulate failures to transition from CLOSED to OPEN
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        }
        assertEquals("OPEN", circuitBreaker.getState(), "Circuit breaker should transition to OPEN state");

        // Wait for the configured wait duration
        Thread.sleep(2000);

        // Trigger transition to HALF_OPEN
        assertTrue(circuitBreaker.allowRequest(), "Request should be allowed after wait duration in OPEN state");
        assertEquals("HALF_OPEN", circuitBreaker.getState(), "Circuit breaker should transition to HALF_OPEN state");

        // Simulate a failure in HALF_OPEN state, transitioning back to OPEN
        circuitBreaker.onError(100, TimeUnit.MILLISECONDS);
        assertEquals("OPEN", circuitBreaker.getState(), "Circuit breaker should transition back to OPEN state on failure in HALF_OPEN");

        // Wait for the configured wait duration again
        Thread.sleep(2000);

        // Trigger transition to HALF_OPEN again
        assertTrue(circuitBreaker.allowRequest(), "Request should be allowed after wait duration in OPEN state");
        assertEquals("HALF_OPEN", circuitBreaker.getState(), "Circuit breaker should transition to HALF_OPEN state again");

        // Simulate successful requests in HALF_OPEN to transition back to CLOSED
        for (int i = 0; i < 5; i++) {
            circuitBreaker.onSuccess(200, TimeUnit.MILLISECONDS);
        }
        assertEquals("CLOSED", circuitBreaker.getState(), "Circuit breaker should transition back to CLOSED state after successful requests");
    }

}
