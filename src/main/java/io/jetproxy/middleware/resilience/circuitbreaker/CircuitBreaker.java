package io.jetproxy.middleware.resilience.circuitbreaker;

import io.jetproxy.middleware.resilience.ResilienceInterface;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A highly concurrent, non-blocking Circuit Breaker implementation
 * designed for high-throughput environments.
 *
 * States and Transitions:
 *          +--------------------+
 *          |                    |
 *          v                    |
 *      +---------+    (failure detected)    +--------+
 *      |  CLOSED |------------------------->|  OPEN  |
 *      +---------+                         +--------+
 *           |                                   |
 *           |      (waitDuration expires)      |
 *           |---------------------------------->| HALF_OPEN |
 *           |                                   +----------+
 *           |                                    ^      |
 *           | (success)                          |      | (failure)
 *           |                                    |      v
 *           +------------------------------------+   +--------+
 *                        (recovered)                 |  OPEN  |
 *                                                    +--------+
 */
public class CircuitBreaker implements ResilienceInterface {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final double failureRateThreshold;
    private final double slowCallRateThreshold;
    private final Duration slowCallDurationThreshold;
    private final Duration waitDurationInOpenState;
    private final int permittedNumberOfCallsInHalfOpenState;
    private final int minimumNumberOfCalls;

    private volatile State state = State.CLOSED; // The current state of the Circuit Breaker
    private final AtomicLong successCalls = new AtomicLong(0); // Counter for successful calls
    private final AtomicLong failureCalls = new AtomicLong(0); // Counter for failed calls
    private final AtomicLong slowCalls = new AtomicLong(0); // Counter for slow calls
    private final AtomicLong halfOpenCalls = new AtomicLong(0); // Counter for calls in HALF_OPEN state
    private final AtomicLong lastStateChangeTime = new AtomicLong(0); // Tracks the last state change time
    private final String name;

    /**
     * Constructor to initialize the Circuit Breaker with a configuration.
     *
     * @param config Configuration for thresholds and limits.
     */
    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.failureRateThreshold = config.getFailureRateThreshold();
        this.slowCallRateThreshold = config.getSlowCallRateThreshold();
        this.slowCallDurationThreshold = config.getSlowCallDurationThreshold();
        this.waitDurationInOpenState = config.getWaitDurationInOpenState();
        this.permittedNumberOfCallsInHalfOpenState = config.getPermittedNumberOfCallsInHalfOpenState();
        this.minimumNumberOfCalls = config.getMinimumNumberOfCalls();
        this.name = name;
    }

    /**
     * Determines whether a request is allowed based on the current state.
     *
     * @return true if the request is allowed, false otherwise.
     */
    @Override
    public boolean allowRequest() {
        long now = System.currentTimeMillis();

        if (state == State.OPEN) {
            // Check if the wait duration has elapsed
            if (now - lastStateChangeTime.get() >= waitDurationInOpenState.toMillis()) {
                if (lastStateChangeTime.compareAndSet(lastStateChangeTime.get(), now)) {
                    // Transition to HALF_OPEN state
                    state = State.HALF_OPEN;
                    halfOpenCalls.set(0);
                    return true; // Allow the first request in HALF_OPEN
                }
            }
            return false; // Reject all requests in OPEN state
        }

        if (state == State.HALF_OPEN) {
            // Allow limited requests in HALF_OPEN state
            long currentCalls = halfOpenCalls.incrementAndGet();
            if (currentCalls > permittedNumberOfCallsInHalfOpenState) {
                return false; // Reject requests exceeding the limit
            }
            return true;
        }

        return true; // CLOSED state, always allow requests
    }

    /**
     * Records a successful request.
     *
     * @param duration Duration of the request.
     * @param unit     Time unit of the duration.
     */
    @Override
    public void onSuccess(long duration, TimeUnit unit) {
        long durationMillis = unit.toMillis(duration);

        if (state == State.HALF_OPEN) {
            // Evaluate success in HALF_OPEN state
            evaluateHalfOpenState(true);
        } else {
            if (durationMillis > slowCallDurationThreshold.toMillis()) {
                slowCalls.incrementAndGet(); // Record slow call
            } else {
                successCalls.incrementAndGet(); // Record successful call
            }
            evaluateState(); // Evaluate whether state transition is needed
        }
    }

    /**
     * Records a failed request.
     *
     * @param duration Duration of the request.
     * @param unit     Time unit of the duration.
     */
    @Override
    public void onError(long duration, TimeUnit unit) {
        if (state == State.HALF_OPEN) {
            // Evaluate failure in HALF_OPEN state
            evaluateHalfOpenState(false);
        } else {
            failureCalls.incrementAndGet(); // Record failed call
            evaluateState(); // Evaluate whether state transition is needed
        }
    }

    /**
     * Returns the current state of the Circuit Breaker.
     *
     * @return The current state as a string.
     */
    @Override
    public String getState() {
        return state.name();
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Evaluates whether the Circuit Breaker should transition to OPEN state
     * based on failure and slow call rates.
     */
    private void evaluateState() {
        long totalCalls = successCalls.get() + failureCalls.get() + slowCalls.get();
        if (totalCalls < minimumNumberOfCalls) return; // Insufficient data to evaluate

        double failureRate = (double) failureCalls.get() / totalCalls * 100;
        double slowCallRate = (double) slowCalls.get() / totalCalls * 100;

        // Transition to OPEN if thresholds are exceeded
        if (failureRate > failureRateThreshold || slowCallRate > slowCallRateThreshold) {
            transitionToOpen();
        }
    }

    /**
     * Evaluates the behavior in HALF_OPEN state and transitions to CLOSED or OPEN.
     *
     * @param isSuccess true if the call was successful, false otherwise.
     */
    private void evaluateHalfOpenState(boolean isSuccess) {
        if (!isSuccess) {
            transitionToOpen(); // Failure transitions back to OPEN
        } else {
            long successfulCalls = successCalls.incrementAndGet();
            if (successfulCalls >= permittedNumberOfCallsInHalfOpenState) {
                transitionToClosed(); // Success threshold met, transition to CLOSED
            }
        }
    }

    /**
     * Transitions the Circuit Breaker to OPEN state.
     */
    private void transitionToOpen() {
        state = State.OPEN;
        lastStateChangeTime.set(System.currentTimeMillis());
        resetCounts(); // Reset counters for the next evaluation cycle
    }

    /**
     * Transitions the Circuit Breaker to CLOSED state.
     */
    private void transitionToClosed() {
        state = State.CLOSED;
        resetCounts(); // Reset counters for the next evaluation cycle
    }

    /**
     * Resets all call counters to their initial state.
     */
    private void resetCounts() {
        successCalls.set(0);
        failureCalls.set(0);
        slowCalls.set(0);
        halfOpenCalls.set(0);
    }
}
