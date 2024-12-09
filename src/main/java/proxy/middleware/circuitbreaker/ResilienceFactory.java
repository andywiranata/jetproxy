package proxy.middleware.circuitbreaker;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import proxy.context.AppConfig;
import proxy.logger.DebugAwareLogger;

import java.time.Duration;

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

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        return registry.circuitBreaker(name);
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

        RetryRegistry registry = RetryRegistry.of(config);
        return registry.retry(name);
    }

    /**
     * Creates a RateLimiter instance.
     *
     * @param name      the name of the RateLimiter
     * @param rlConfig  the RateLimiter configuration from AppConfig
     * @return the RateLimiter instance
     */
    public static RateLimiter createRateLimiter(String name, AppConfig.RateLimiter rlConfig) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(rlConfig.getRefreshPeriod()))
                .limitForPeriod(rlConfig.getLimitForPeriod())
                .timeoutDuration(Duration.ofMillis(rlConfig.getTimeoutDuration()))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        return registry.rateLimiter(name);
    }

    /**
     * Creates a Bulkhead instance.
     *
     * @param name      the name of the Bulkhead
     * @param bhConfig  the Bulkhead configuration from AppConfig
     * @return the Bulkhead instance
     */
    public static Bulkhead createBulkhead(String name, AppConfig.Bulkhead bhConfig) {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(bhConfig.getMaxConcurrentCalls())
                .maxWaitDuration(Duration.ofMillis(bhConfig.getMaxWaitDuration()))
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        return registry.bulkhead(name);
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
        logger.debug("setup resilience with id: {}", proxy.getUuid());
        AppConfig.Middleware middleware = proxy.getMiddleware();

        CircuitBreaker circuitBreaker = middleware.hasCircuitBreaker()
                ? createCircuitBreaker("resilience::circuitbreaker::"+ proxy.getUuid(), middleware.getCircuitBreaker())
                : null;

        Retry retry = middleware.hasRetry()
                ? createRetry("resilience::retry::"+ proxy.getUuid(), middleware.getRetry())
                : null;

        RateLimiter rateLimiter = middleware.hasRateLimiter()
                ? createRateLimiter("resilience::ratelimiter::"+ proxy.getUuid(), middleware.getRateLimiter())
                : null;

        Bulkhead bulkhead = middleware.hasBulkHead()
                ? createBulkhead("resilience::bulkhead::"+ proxy.getUuid(), middleware.getBulkhead())
                : null;

        return new ResilienceUtil(circuitBreaker, retry, rateLimiter, bulkhead);
    }


}
