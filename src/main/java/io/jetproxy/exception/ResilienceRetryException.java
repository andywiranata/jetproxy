package io.jetproxy.exception;

/**
 * Exception thrown when retry operations fail in Resilience mechanisms.
 */
public class ResilienceRetryException extends JetProxyException {
    public ResilienceRetryException(String message) {
        super(message);
    }

    public ResilienceRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}