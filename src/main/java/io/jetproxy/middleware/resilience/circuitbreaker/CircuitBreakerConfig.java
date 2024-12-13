package io.jetproxy.middleware.resilience.circuitbreaker;

import lombok.Getter;

import java.time.Duration;

/**
 * Configuration for the Circuit Breaker, encapsulating thresholds and limits.
 */
@Getter
public class CircuitBreakerConfig {
    private final double failureRateThreshold;
    private final double slowCallRateThreshold;
    private final Duration slowCallDurationThreshold;
    private final Duration waitDurationInOpenState;
    private final int permittedNumberOfCallsInHalfOpenState;
    private final int minimumNumberOfCalls;

    private CircuitBreakerConfig(Builder builder) {
        this.failureRateThreshold = builder.failureRateThreshold;
        this.slowCallRateThreshold = builder.slowCallRateThreshold;
        this.slowCallDurationThreshold = builder.slowCallDurationThreshold;
        this.waitDurationInOpenState = builder.waitDurationInOpenState;
        this.permittedNumberOfCallsInHalfOpenState = builder.permittedNumberOfCallsInHalfOpenState;
        this.minimumNumberOfCalls = builder.minimumNumberOfCalls;
    }

    public static Builder custom() {
        return new Builder();
    }

    /**
     * Builder class for fluent configuration of Circuit Breaker.
     */
    public static class Builder {
        private double failureRateThreshold = 50.0;
        private double slowCallRateThreshold = 100.0;
        private Duration slowCallDurationThreshold = Duration.ofMillis(500);
        private Duration waitDurationInOpenState = Duration.ofSeconds(2);
        private int permittedNumberOfCallsInHalfOpenState = 10;
        private int minimumNumberOfCalls = 100;

        public Builder failureRateThreshold(double threshold) {
            this.failureRateThreshold = threshold;
            return this;
        }

        public Builder slowCallRateThreshold(double threshold) {
            this.slowCallRateThreshold = threshold;
            return this;
        }

        public Builder slowCallDurationThreshold(Duration threshold) {
            this.slowCallDurationThreshold = threshold;
            return this;
        }

        public Builder waitDurationInOpenState(Duration duration) {
            this.waitDurationInOpenState = duration;
            return this;
        }

        public Builder permittedNumberOfCallsInHalfOpenState(int calls) {
            this.permittedNumberOfCallsInHalfOpenState = calls;
            return this;
        }

        public Builder minimumNumberOfCalls(int calls) {
            this.minimumNumberOfCalls = calls;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(this);
        }
    }
}
