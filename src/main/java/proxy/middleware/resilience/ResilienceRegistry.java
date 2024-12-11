package proxy.middleware.resilience;

import proxy.middleware.resilience.circuitbreaker.CircuitBreaker;
import proxy.middleware.resilience.circuitbreaker.CircuitBreakerConfig;
import proxy.middleware.resilience.retry.Retry;
import proxy.middleware.resilience.retry.RetryConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry to manage instances of resilience mechanisms (e.g., Circuit Breaker, Retry).
 */
public class ResilienceRegistry {

    private final ConcurrentMap<String, ResilienceInterface> registry = new ConcurrentHashMap<>();

    /**
     * Registers a new resilience mechanism instance with the given name.
     *
     * @param name       The name of the resilience mechanism instance.
     * @param resilience The resilience mechanism instance.
     */
    public void register(String name, ResilienceInterface resilience) {
        registry.put(name, resilience);
    }

    /**
     * Retrieves an existing resilience mechanism instance by name.
     *
     * @param name The name of the resilience mechanism instance.
     * @return The resilience mechanism instance or null if not found.
     */
    public ResilienceInterface get(String name) {
        return registry.get(name);
    }

    /**
     * Removes a resilience mechanism instance by name.
     *
     * @param name The name of the resilience mechanism instance to remove.
     */
    public void remove(String name) {
        registry.remove(name);
    }

    /**
     * Creates and registers a Circuit Breaker instance with the given name and configuration.
     *
     * @param name   The name of the Circuit Breaker instance.
     * @param config The Circuit Breaker configuration.
     * @return The created Circuit Breaker instance.
     */
    public CircuitBreaker createCircuitBreaker(String name, CircuitBreakerConfig config) {
        CircuitBreaker circuitBreaker = new CircuitBreaker(name, config);
        register(name, circuitBreaker);
        return circuitBreaker;
    }

    /**
     * Creates and registers a Retry instance with the given name and configuration.
     *
     * @param name   The name of the Retry instance.
     * @param config The Retry configuration.
     * @return The created Retry instance.
     */
    public Retry createRetry(String name, RetryConfig config) {
        Retry retry = new Retry(name, config);
        register(name, retry);
        return retry;
    }
}
