package io.jetproxy.middleware.resilience;

import io.jetproxy.exception.ResilienceCircuitBreakerException;
import io.jetproxy.exception.ResilienceRateLimitException;
import io.jetproxy.exception.ResilienceRetryException;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.resilience.retry.Retry;
import io.jetproxy.middleware.resilience.circuitbreaker.CircuitBreaker;
import io.jetproxy.middleware.resilience.ratelimiter.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;

/**
 * A utility class to manage and execute operations with resilience mechanisms
 * like Retry, CircuitBreaker, and RateLimiter.
 */
public class ResilienceUtil {

    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ResilienceUtil.class);

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;

    ResilienceUtil() {
        this.circuitBreaker = null;
        this.retry =  null;
        this.rateLimiter = null;
    }
    /**
     * Constructs a ResilienceUtil with the given resilience components.
     *
     * @param circuitBreaker the CircuitBreaker instance
     * @param retry          the Retry instance
     * @param rateLimiter    the RateLimiter instance
     */
    public ResilienceUtil(CircuitBreaker circuitBreaker, Retry retry, RateLimiter rateLimiter) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Executes a runnable operation with resilience protections.
     *
     * @param runnable the operation to execute
     */
    public void execute(Runnable runnable) {
        Runnable resilientRunnable = runnable;

        // Apply RateLimiter logic
        if (rateLimiter != null && !rateLimiter.tryConsume()) {
            throw new ResilienceRateLimitException("Rate limit exceeded");
        }

        // Apply Retry logic
        if (retry != null) {
            Runnable originalRunnable = resilientRunnable;
            resilientRunnable = () -> {
                while (retry.allowRequest()) {
                    try {
                        originalRunnable.run();
                        break;
                    } catch (Exception ex) {
                        if (!retry.allowRequest()) {
                            throw new ResilienceRetryException("Retry attempts exceeded", ex);
                        }
                    }
                }
            };
        }

        // Apply CircuitBreaker logic
        if (circuitBreaker != null) {
            if (circuitBreaker.allowRequest()) {
                Runnable finalRunnable = resilientRunnable;
                resilientRunnable = () -> {
                    try {
                        finalRunnable.run();
                    } catch (Exception ex) {
                        circuitBreaker.onError(0, TimeUnit.MILLISECONDS); // Assume 0 duration if unknown
                        throw new ResilienceCircuitBreakerException("Circuit Breaker Exception", ex);
                    }
                };
            } else {
                throw new ResilienceCircuitBreakerException("Circuit Breaker Open");
            }
        }

        // Execute the final resilient runnable
        try {
            resilientRunnable.run();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            // Wrap exceptions to ensure proper propagation
            throw ex;
        } catch (Exception ex) {
            // Re-wrap non-runtime exceptions
            throw new RuntimeException(ex);
        }
    }


    /**
     * Handles the response logic for resilience mechanisms.
     *
     * @param clientRequest the client request
     * @param responseStatus the HTTP response status
     * @param exception any exception that occurred during processing
     */
    public void handleHttpResponse(HttpServletRequest clientRequest, int responseStatus, Throwable exception) {
        Long startTime = (Long) clientRequest.getAttribute("startTime");
        long duration = (startTime != null) ? System.nanoTime() - startTime : 0;

        // CircuitBreaker logic
        if (circuitBreaker != null) {
            if (exception != null || responseStatus < 200 || responseStatus >= 300) {
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS); // Mark as error
            } else {
                circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS); // Mark as success
            }
        }

        // Retry logic
        if (retry != null) {
            if (exception != null || responseStatus < 200 || responseStatus >= 300) {
                retry.onError(duration, TimeUnit.NANOSECONDS); // Mark as error
            } else {
                retry.onSuccess(duration, TimeUnit.NANOSECONDS); // Mark as success
            }
        }

        if (rateLimiter != null) {

        }

        // RateLimiter: No specific response handling needed since it applies before execution
    }

    /**
     * Checks if the CircuitBreaker is open.
     *
     * @return true if CircuitBreaker is open, false otherwise
     */
    public boolean isCircuitBreakerNotAllowRequest() {
        return circuitBreaker != null && !circuitBreaker.allowRequest();
    }


}
