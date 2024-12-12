package proxy.middleware.resilience;

import proxy.context.AppConfig;
import proxy.logger.DebugAwareLogger;
import proxy.middleware.resilience.circuitbreaker.CircuitBreaker;
import proxy.middleware.resilience.circuitbreaker.CircuitBreakerConfig;
import proxy.middleware.resilience.retry.Retry;
import proxy.middleware.resilience.retry.RetryConfig;

import java.time.Duration;

/**
 * Factory for creating resilience components (Circuit Breaker, Retry, RateLimiter, Bulkhead).
 */
public class ResilienceFactory {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ResilienceFactory.class);

    /**
     * Creates a CircuitBreaker instance.
     *
     * @param name     the name of the CircuitBreaker
     * @param cbConfig the CircuitBreaker configuration from AppConfig
     * @return the CircuitBreaker instance
     */
    public static CircuitBreaker createCircuitBreaker(String name, AppConfig.CircuitBreaker cbConfig) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureThreshold())
                .slowCallRateThreshold(cbConfig.getSlowCallThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(cbConfig.getSlowCallDuration()))
                .waitDurationInOpenState(Duration.ofMillis(cbConfig.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedNumberOfCallsInHalfOpenState())
                .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                .build();

        return new CircuitBreaker(name, config);
    }

    /**
     * Creates a Retry instance.
     *
     * @param name       the name of the Retry
     * @param retryConfig the Retry configuration from AppConfig
     * @return the Retry instance
     */
    public static Retry createRetry(String name, AppConfig.Retry retryConfig) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryConfig.getMaxAttempts())
                .waitDuration(Duration.ofMillis(retryConfig.getWaitDuration()))

                .build();

        return new Retry(name, config);
    }


    /**
     * Creates a composite resilience configuration with all components.
     *
     * @param proxy the proxy configuration from AppConfig
     * @return the ResilienceUtil instance with all configured components
     */
    public static ResilienceUtil createResilienceUtil(AppConfig.Proxy proxy) {
        if (!proxy.hasMiddleware()) {
            return null;
        }
        logger.debug("Setting up resilience with id: {}", proxy.getUuid());
        AppConfig.Middleware middleware = proxy.getMiddleware();

        CircuitBreaker circuitBreaker = middleware.hasCircuitBreaker()
                ? createCircuitBreaker("resilience::circuitbreaker::" + proxy.getUuid(), middleware.getCircuitBreaker())
                : null;

        Retry retry = middleware.hasRetry()
                ? createRetry("resilience::retry::" + proxy.getUuid(), middleware.getRetry())
                : null;

        return new ResilienceUtil(circuitBreaker, retry);
    }
}
