package proxy.middleware.circuitbreaker;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import proxy.logger.DebugAwareLogger;

import java.util.concurrent.TimeUnit;

@Getter
public class ResilienceUtil {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ResilienceUtil.class);
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final Bulkhead bulkhead;

    /**
     * Constructs a ResilienceUtil with the given resilience components.
     *
     * @param circuitBreaker the CircuitBreaker instance
     * @param retry          the Retry instance
     * @param rateLimiter    the RateLimiter instance
     * @param bulkhead       the Bulkhead instance
     */
    public ResilienceUtil(CircuitBreaker circuitBreaker,
                          Retry retry,
                          RateLimiter rateLimiter,
                          Bulkhead bulkhead) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
    }

    /**
     * Executes a runnable with resilience protections.
     *
     * @param runnable the runnable to execute
     */
    public void execute(Runnable runnable) {
        Runnable resilientRunnable = runnable;

        // Apply Retry
        if (retry != null) {
            resilientRunnable = Retry.decorateRunnable(retry, resilientRunnable);
        }

        // Apply RateLimiter
        if (rateLimiter != null) {
            resilientRunnable = RateLimiter.decorateRunnable(rateLimiter, resilientRunnable);
        }

        // Apply CircuitBreaker
        if (circuitBreaker != null) {
            logger.debug("circuit breaker executed state: {} {}", circuitBreaker.getName(), circuitBreaker.getState());
            resilientRunnable = CircuitBreaker.decorateRunnable(circuitBreaker, resilientRunnable);
        }

        // Apply Bulkhead
        if (bulkhead != null) {
            resilientRunnable = Bulkhead.decorateRunnable(bulkhead, resilientRunnable);
        }
        resilientRunnable.run();
    }

    public void handleHttpResponse(HttpServletRequest clientRequest, int responseStatus, Throwable exception) {
        if (!hasCircuitBreaker() &&
                !hasBulkhead() &&
                !hasRateLimiter() &&
                !hasBulkhead()) {
            return; // No resilience components configured
        }

        // Retrieve start time from the request
        Long startTime = (Long) clientRequest.getAttribute("startTime");
        long duration = (startTime != null) ? System.nanoTime() - startTime : 0;

        // Log the duration
        logger.debug("Request duration: {} ms", TimeUnit.NANOSECONDS.toMillis(duration));

        // Handle CircuitBreaker logic
        if (hasCircuitBreaker()) {
            if (exception != null) {
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, exception); // Mark as error
            } else if (responseStatus >= 200 && responseStatus < 300) {
                circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS); // Mark as success
            } else {
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, new RuntimeException("HTTP error: " + responseStatus));
            }
        }

        // Handle Retry logic (optional logging or metrics)
        if (hasRetry()) {
            // Retry metrics are mostly automatic, but logging can provide visibility
            logger.debug("Retry metrics: Successful calls without retry = {}, Failed calls with retry = {}",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                    retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
        }

        // Handle RateLimiter logic
        if (hasRateLimiter()) {
            // Log RateLimiter metrics (permissions remaining, etc.)
            logger.debug("RateLimiter metrics: Available permissions = {}, Waiting threads = {}",
                    rateLimiter.getMetrics().getAvailablePermissions(),
                    rateLimiter.getMetrics().getNumberOfWaitingThreads());
        }

        // Handle Bulkhead logic
        if (hasBulkhead()) {
            // Log Bulkhead metrics (available concurrent calls, etc.)
            logger.debug("Bulkhead metrics: Available concurrent calls = {}, Max concurrent calls = {}",
                    bulkhead.getMetrics().getAvailableConcurrentCalls(),
                    bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        }
    }

    public boolean hasCircuitBreaker() {
        return circuitBreaker != null;
    }

    public boolean hasRetry() {
        return retry != null;
    }

    public boolean hasRateLimiter() {
        return rateLimiter != null;
    }

    public boolean hasBulkhead() {
        return bulkhead != null;
    }

    public boolean isCircuitBreakerOpen() {
        if (!hasCircuitBreaker()) {
            return false;
        }
        logger.debug("CircuitBreaker '{}': State={}, BufferedCalls={}, SuccessfulCalls={}, FailedCalls={}, SlowCalls={}, FailureRate={}%, SlowCallRate={}%",
                circuitBreaker.getName(),
                circuitBreaker.getState(),
                circuitBreaker.getMetrics().getNumberOfBufferedCalls(),
                circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
                circuitBreaker.getMetrics().getNumberOfFailedCalls(),
                circuitBreaker.getMetrics().getNumberOfSlowCalls(),
                circuitBreaker.getMetrics().getFailureRate(),
                circuitBreaker.getMetrics().getSlowCallRate());
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public boolean isRetryEnabled() {
        if (!hasRateLimiter()) {
            return false;
        }
        return retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt() >= 0;
    }

    public boolean isBulkheadAvailable() {
        if (!hasBulkhead()) {
            return false;
        }
        return bulkhead.getMetrics().getAvailableConcurrentCalls() > 0;
    }

    public boolean isRateLimiterAvailable() {
        if (!hasRateLimiter()) {
            return false;
        }
        return rateLimiter.getMetrics().getAvailablePermissions() > 0;
    }
}
