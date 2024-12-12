package proxy.exception;

/**
 * Exception thrown when Circuit Breaker operations fail in Resilience mechanisms.
 */
public class ResilienceCircuitBreakerException extends JetProxyException {
    public ResilienceCircuitBreakerException(String message) {
        super(message);
    }

    public ResilienceCircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
}