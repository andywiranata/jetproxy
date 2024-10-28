package proxy.middleware.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import proxy.context.AppConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class CircuitBreakerFactory {
    private static final Map<String, CircuitBreakerUtil> cache = new ConcurrentHashMap<>();

    public static CircuitBreakerUtil createCircuitBreaker(String name, CircuitBreakerConfig config) {
        return cache.computeIfAbsent(name, key -> {
            CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
            return new CircuitBreakerUtil(name, registry);
        });
    }

    public static CircuitBreakerUtil createDefaultCircuitBreaker(String name) {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate threshold
                .slowCallRateThreshold(50) // 50% slow call rate threshold
                .slowCallDurationThreshold(Duration.ofMillis(2000)) // 2 seconds threshold for slow calls
                .waitDurationInOpenState(Duration.ofMillis(10000)) // 10-second wait in open state
                .permittedNumberOfCallsInHalfOpenState(3) // 3 calls in half-open state
                .minimumNumberOfCalls(5) // Minimum of 5 calls before evaluating
                .build();

        return createCircuitBreaker(name, defaultConfig);
    }

    public static CircuitBreakerUtil createCircuitBreaker(String name,
                                                                AppConfig.CircuitBreaker cbConfig) {
        CircuitBreakerConfig customConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureThreshold())
                .slowCallRateThreshold(cbConfig.getSlowCallThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(cbConfig.getSlowCallDuration()))
                .waitDurationInOpenState(Duration.ofMillis(cbConfig.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedNumberOfCallsInHalfOpenState())
                .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                .build();

        return createCircuitBreaker(name, customConfig);
    }
}
