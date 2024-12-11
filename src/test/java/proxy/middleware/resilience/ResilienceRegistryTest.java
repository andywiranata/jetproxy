package proxy.middleware.resilience;

import proxy.middleware.resilience.circuitbreaker.CircuitBreaker;
import proxy.middleware.resilience.circuitbreaker.CircuitBreakerConfig;
import proxy.middleware.resilience.retry.Retry;
import proxy.middleware.resilience.retry.RetryConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ResilienceRegistryTest {

    public static void main(String[] args) {
        // Create a registry
        ResilienceRegistry registry = new ResilienceRegistry();

        // Create and register a Circuit Breaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofMillis(500))
                .waitDurationInOpenState(Duration.ofMillis(2000))
                .permittedNumberOfCallsInHalfOpenState(5)
                .minimumNumberOfCalls(10)
                .build();

        CircuitBreaker circuitBreaker = registry.createCircuitBreaker("service-circuit-breaker", circuitBreakerConfig);

        // Create and register a Retry
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();

        Retry retry = registry.createRetry("service-retry", retryConfig);

        // Retrieve instances from the registry
        ResilienceInterface retrievedCircuitBreaker = registry.get("service-circuit-breaker");
        ResilienceInterface retrievedRetry = registry.get("service-retry");

        // Use the Circuit Breaker
        if (retrievedCircuitBreaker.allowRequest()) {
            System.out.println("Circuit Breaker allows the request.");
        } else {
            System.out.println("Circuit Breaker blocks the request.");
        }

        // Use the Retry
        while (retrievedRetry.allowRequest()) {
            System.out.println("Retrying...");
            // Simulate a failure
            retrievedRetry.onError(0, TimeUnit.MILLISECONDS);
        }
        System.out.println("Retries exhausted.");
    }
}
