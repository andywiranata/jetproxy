package io.jetproxy.middleware.resilience.ratelimiter;

import lombok.Getter;
import java.time.Duration;

@Getter
public class RateLimiterConfig {
    private final Duration limitRefreshPeriod;
    private final int limitForPeriod;
    private final int maxBurstCapacity;

    private RateLimiterConfig(Builder builder) {
        this.limitRefreshPeriod = builder.limitRefreshPeriod;
        this.limitForPeriod = builder.limitForPeriod;
        this.maxBurstCapacity = builder.maxBurstCapacity;
    }

    public static Builder custom() {
        return new Builder();
    }
    public static class Builder {
        private Duration limitRefreshPeriod = Duration.ofMillis(1000);
        private int limitForPeriod = 10;
        private int maxBurstCapacity = 20;

        public Builder limitRefreshPeriod(Duration period) {
            if (period.isZero() || period.isNegative()) {
                throw new IllegalArgumentException("limitRefreshPeriod must be greater than 0");
            }
            this.limitRefreshPeriod = period;
            return this;
        }

        public Builder limitForPeriod(int limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("limitForPeriod must be non-negative");
            }
            this.limitForPeriod = limit;
            return this;
        }

        public Builder maxBurstCapacity(int maxBurstCapacity) {
            if (maxBurstCapacity < 0) {
                throw new IllegalArgumentException("maxBurstCapacity must be non-negative");
            }
            this.maxBurstCapacity = maxBurstCapacity;
            return this;
        }

        public RateLimiterConfig build() {
            if (maxBurstCapacity < limitForPeriod) {
                throw new IllegalArgumentException("maxBurstCapacity must be >= limitForPeriod");
            }
            return new RateLimiterConfig(this);
        }
    }

}
