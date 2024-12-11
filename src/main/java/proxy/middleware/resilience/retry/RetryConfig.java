package proxy.middleware.resilience.retry;

import lombok.Getter;

import java.time.Duration;

/**
 * Configuration class for the Retry mechanism.
 */
@Getter
public class RetryConfig {

    private final int maxAttempts;
    private final Duration waitDuration;

    private RetryConfig(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.waitDuration = builder.waitDuration;
    }

    /**
     * Creates a new Builder instance for custom configuration.
     *
     * @return a new Builder instance.
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Builder for RetryConfig.
     */
    public static class Builder {
        private int maxAttempts = 3; // Default: 3 attempts
        private Duration waitDuration = Duration.ofMillis(500); // Default: 500ms wait

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder waitDuration(Duration waitDuration) {
            this.waitDuration = waitDuration;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}
