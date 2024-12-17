---
sidebar_position: 8
---

# Rate Limiter

A Rate Limiter is a middleware component designed to control the flow of incoming requests to a system. It ensures the system isn't overwhelmed by traffic spikes or abusive usage patterns, maintaining optimal performance and reliability.

```

                 +-------------------+
                 | Incoming Request  |
                 +-------------------+
                           |
                           v
                  +-------------------+
                  | Rate Limiter      |
                  | Check Tokens      |
                  +-------------------+
                      /       \
              Tokens Available  Tokens Exhausted
                   /                 \
          Allow Request           Reject Request
           /      \                  |
   +---------------------+      +-------------------+
   | Forward to Backend  |      | Send 429 Response |
   +---------------------+      +-------------------+

```

## Configuring Example

```yaml
proxies:
  - path: /user
    service: userApi
    middleware:
      rateLimiter:
        enabled: true
        limitRefreshPeriod: 2000 
        limitForPeriod: 5
        maxBurstCapacity: 10
```

## Explanation of Each Rate Limit State

### **ENABLED**
- **Definition**: Indicates whether the Rate Limiter is active for a specific proxy route.
- **Behavior**:
  - When set to `true`, all incoming requests are subject to rate limiting.
  - When set to `false`, requests bypass the rate-limiting logic.
- **Purpose**: Provides control over whether rate limiting should apply to a particular route.

### **LIMIT_REFRESH_PERIOD**
- **Definition**: The interval at which the bucket refills with new tokens.
- **Behavior**:
  - Configured in milliseconds.
  - At each interval, `limitForPeriod` tokens are added to the bucket.
  - The bucket accumulates tokens up to `maxBurstCapacity` if unused.
- **Purpose**: Ensures steady traffic flow while preventing abuse.
- **Example**: `limitRefreshPeriod: 2000` refills tokens every 2 seconds.

### **LIMIT_FOR_PERIOD**
- **Definition**: The number of tokens added to the bucket during each refresh period.
- **Behavior**:
  - Represents the maximum number of requests allowed per interval (`limitRefreshPeriod`).
  - Each request consumes one token.
  - Requests are denied when tokens are exhausted.
- **Purpose**: Defines the rate of allowed requests to maintain system stability.
- **Example**: `limitForPeriod: 5` allows 5 requests per 2-second interval.

### **MAX_BURST_CAPACITY**
- **Definition**: The maximum number of tokens the bucket can hold, including unused tokens from previous periods.
- **Behavior**:
  - Allows the system to handle sudden bursts of traffic.
  - Tokens beyond this value are discarded during a refill.
- **Purpose**: Provides flexibility to accommodate temporary traffic spikes.
- **Example**: `maxBurstCapacity: 6` allows up to 6 requests in a single burst.

## Workflow

1. **Token Check**:
   - Each incoming request consumes a token.
   - If tokens are available, the request is allowed.
   - If no tokens are available, the request is denied with a `429 Too Many Requests` response.

2. **Token Refill**:
   - Tokens are replenished at intervals defined by `limitRefreshPeriod`.
   - Up to `limitForPeriod` tokens are added, without exceeding `maxBurstCapacity`.

3. **Burst Handling**:
   - If tokens were not fully utilized in previous periods, they accumulate up to `maxBurstCapacity`.
   - This allows temporary spikes in traffic to be handled gracefully.