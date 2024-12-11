package proxy.middleware.resilience;

import java.util.concurrent.TimeUnit;

/**
 * A generic interface for resilience mechanisms such as Circuit Breaker, Retry, or Rate Limiter.
 */
public interface ResilienceInterface {
    /**
     * Determines if a request is allowed to proceed based on the current state.
     *
     * @return true if the request is allowed; false otherwise.
     */
    boolean allowRequest();

    /**
     * Marks a request as successful, including its execution duration.
     *
     * @param duration The duration of the request.
     * @param unit     The time unit of the duration.
     */
    void onSuccess(long duration, TimeUnit unit);

    /**
     * Marks a request as failed, including its execution duration.
     *
     * @param duration The duration of the request.
     * @param unit     The time unit of the duration.
     */
    void onError(long duration, TimeUnit unit);

    /**
     * Retrieves the current state of the resilience mechanism.
     *
     * @return The current state as a String.
     */
    String getState();
    String getName();
}
