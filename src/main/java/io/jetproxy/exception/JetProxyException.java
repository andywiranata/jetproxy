package io.jetproxy.exception;

/**
 * Base exception class for JetProxy-related errors.
 */
public class JetProxyException extends RuntimeException {
    public JetProxyException(String message) {
        super(message);
    }

    public JetProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
