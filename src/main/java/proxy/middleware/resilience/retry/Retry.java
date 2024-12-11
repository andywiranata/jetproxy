package proxy.middleware.resilience.retry;


import proxy.middleware.resilience.ResilienceInterface;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Retry mechanism implementation using ResilienceInterface.
 * It retries a failed operation a limited number of times with configurable delays.
 */
public class Retry implements ResilienceInterface {

    private final int maxRetries; // Maximum number of retries
    private final long delayBetweenRetriesMillis; // Delay between retries
    private final AtomicInteger attemptCounter = new AtomicInteger(0); // Tracks current attempts
    private volatile boolean shouldRetry = true; // Indicates if retries are allowed
    private final String name;

    /**
     * Constructs a Retry instance with the given configuration.
     *
     * @param config The RetryConfig containing maxAttempts and waitDuration.
     */
    public Retry(String name, RetryConfig config) {
        this.maxRetries = config.getMaxAttempts();
        this.delayBetweenRetriesMillis = config.getWaitDuration().toMillis();
        this.name = name;
    }

    /**
     * Determines if the current operation can be retried.
     *
     * @return true if retries are allowed, false otherwise.
     */
    public boolean canRetry() {
        if (!shouldRetry) {
            return false; // Retries exhausted
        }

        if (attemptCounter.incrementAndGet() > maxRetries) {
            shouldRetry = false; // Disable further retries
            return false;
        }

        return true; // Allow the retry
    }

    @Override
    public boolean allowRequest() {
        if (attemptCounter.get() == 0) {
            return true;
        }
        return canRetry();
    }

    @Override
    public void onSuccess(long duration, TimeUnit unit) {
        reset(); // Reset the retry mechanism on success
    }

    @Override
    public void onError(long duration, TimeUnit unit) {
        if (canRetry()) {
            try {
                Thread.sleep(delayBetweenRetriesMillis); // Delay before the next retry
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public String getState() {
        if (!shouldRetry) {
            return "FAILED";
        } else if (attemptCounter.get() == 0) {
            return "SUCCESS"; // Indicate success when counter is reset
        } else {
            return "RETRYING";
        }
    }


    private void reset() {
        attemptCounter.set(0);
        shouldRetry = true;
    }

    public String getName() {
        return name;
    }
}
