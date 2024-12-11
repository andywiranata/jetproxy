package proxy.middleware.resilience;

import jakarta.servlet.http.HttpServletRequest;
import proxy.exception.ResilienceRetryException;
import proxy.logger.DebugAwareLogger;
import proxy.middleware.resilience.circuitbreaker.CircuitBreaker;
import proxy.middleware.resilience.retry.Retry;

import java.util.concurrent.TimeUnit;

/**
 * A utility class to manage and execute operations with resilience mechanisms
 * like Retry, CircuitBreaker, and others.
 */
public class ResilienceUtil {

    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ResilienceUtil.class);

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    /**
     * Constructs a ResilienceUtil with the given resilience components.
     *
     * @param circuitBreaker the CircuitBreaker instance
     * @param retry          the Retry instance
     */
    public ResilienceUtil(CircuitBreaker circuitBreaker, Retry retry) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    /**
     * Executes a runnable operation with resilience protections.
     *
     * @param runnable the operation to execute
     */
    public void execute(Runnable runnable) {
        Runnable resilientRunnable = runnable;

        // Apply Retry logic
        if (retry != null && retry.allowRequest()) {
            resilientRunnable = () -> {
                while (retry.allowRequest()) {
                    try {
                        runnable.run();
                        break;
                    } catch (Exception ex) {
                        if (!retry.allowRequest()) {
                            throw new ResilienceRetryException(ex.getMessage()); // Exceed retry attempts
                        }
                    }
                }
            };
        }

        // Apply CircuitBreaker logic
        if (circuitBreaker != null && circuitBreaker.allowRequest()) {
            resilientRunnable = () -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    throw ex;
                }
            };
        }

        // Execute the final resilient runnable
        resilientRunnable.run();
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

        // Handle CircuitBreaker response logic
        if (circuitBreaker != null) {
            if (exception != null) {
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS); // Mark as error
            } else if (responseStatus >= 200 && responseStatus < 300) {
                circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS); // Mark as success
            } else {
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS); // Mark as failure
            }
        }

        // Log retry metrics (if applicable)
        if (retry != null) {
            logger.debug("Retry state: {}", retry.getState());
        }
    }

    /**
     * Checks if the CircuitBreaker is open.
     *
     * @return true if CircuitBreaker is open, false otherwise
     */
    public boolean isCircuitBreakerOpen() {
        return circuitBreaker != null && !circuitBreaker.allowRequest();
    }

    /**
     * Checks if retries are allowed.
     *
     * @return true if retries are enabled and allowed, false otherwise
     */
    public boolean isRetryEnabled() {
        return retry != null && retry.allowRequest();
    }
}
