package proxy.middleware.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.util.function.Supplier;

public class CircuitBreakerUtil {
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerUtil(String name, CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker(name);
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    // Method to execute a supplier with circuit breaker protection
    public <T> T executeWithCircuitBreaker(Supplier<T> supplier) {
        return circuitBreaker.executeSupplier(supplier);
    }

    // Method to execute a runnable with circuit breaker protection
    public void executeRunnableWithCircuitBreaker(Runnable runnable) {
        circuitBreaker.executeRunnable(runnable);
    }
}


