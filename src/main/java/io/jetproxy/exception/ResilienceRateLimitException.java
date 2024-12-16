package io.jetproxy.exception;

public class ResilienceRateLimitException extends JetProxyException {
    public ResilienceRateLimitException(String message) {
        super(message);
    }

    public ResilienceRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
