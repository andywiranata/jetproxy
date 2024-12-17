package io.jetproxy.middleware.resilience.ratelimiter;

import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {
    private final long limitRefreshPeriodMillis;
    private final int limitForPeriod;
    private final int maxBurstCapacity;
    private final AtomicLong availableTokens;
    private final AtomicLong nextRefillTime;

    public RateLimiter(RateLimiterConfig config) {
        if (config.getLimitRefreshPeriod().isZero() || config.getLimitRefreshPeriod().isNegative()) {
            throw new IllegalArgumentException("limitRefreshPeriod must be greater than 0");
        }
        if (config.getLimitForPeriod() <= 0) {
            throw new IllegalArgumentException("limitForPeriod must be greater than 0");
        }
        this.limitRefreshPeriodMillis = config.getLimitRefreshPeriod().toMillis();
        this.limitForPeriod = config.getLimitForPeriod();
        this.maxBurstCapacity = config.getMaxBurstCapacity();
        this.availableTokens = new AtomicLong(config.getMaxBurstCapacity());
        this.nextRefillTime = new AtomicLong(System.currentTimeMillis() + limitRefreshPeriodMillis);
    }

    /**
     * Attempts to consume a token.
     *
     * @return true if the request is allowed, false otherwise.
     */
    public boolean tryConsume() {
        refillTokensIfNeeded();

        // Attempt to decrement the token count
        long currentTokens;
        do {
            currentTokens = availableTokens.get();
            if (currentTokens <= 0) {
                return false; // No tokens available
            }
        } while (!availableTokens.compareAndSet(currentTokens, currentTokens - 1));

        return true;
    }

    /**
     * Refills tokens based on elapsed time, respecting the maxBurstCapacity.
     */
    private void refillTokensIfNeeded() {
        long now = System.currentTimeMillis();
        long refillTime = nextRefillTime.get();

        if (now >= refillTime) {
            long periodsToRefill = (now - refillTime) / limitRefreshPeriodMillis + 1;
            long newRefillTime = refillTime + periodsToRefill * limitRefreshPeriodMillis;

            // Attempt to update the refill time atomically
            if (nextRefillTime.compareAndSet(refillTime, newRefillTime)) {
                long tokensToAdd = periodsToRefill * limitForPeriod;
                availableTokens.getAndUpdate(tokens -> Math.min(maxBurstCapacity, tokens + tokensToAdd));
            }
        }
    }
}
