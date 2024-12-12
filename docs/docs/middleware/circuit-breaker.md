---
sidebar_position: 7
---

# Circuit Breaker

The circuit breaker safeguards your system by preventing excessive requests from being sent to unhealthy services, thereby avoiding cascading failures.

In normal operations, when the system is healthy, the circuit remains closed, allowing requests to flow as expected. However, when the system becomes unhealthy, the circuit opens, stopping requests from being forwarded and instead redirecting them to a fallback mechanism.

The circuit breaker continuously monitors the services to determine their health and adjust its state accordingly.

```
A highly concurrent, non-blocking Circuit Breaker implementation designed for high-throughput environments.
States and Transitions:
         +--------------------+
         |                    |
         v                    |
     +---------+    (failure detected)    +--------+
     |  CLOSED |------------------------->|  OPEN  |
     +---------+                         +--------+
          |                                   |
          |      (waitDuration expires)      |
          |---------------------------------->| HALF_OPEN |
          |                                   +----------+
          |                                    ^      |
          | (success)                          |      | (failure)
          |                                    |      v
          +------------------------------------+   +--------+
                       (recovered)                 |  OPEN  |
                                                   +--------+

```

> Each route's proxy is assigned its own dedicated circuit breaker instance. This means that the state of one circuit breaker (open or closed) is independent of others, allowing one to be open while another remains closed.

## Configuring Example

```yaml

proxies:
  - path: /user
    service: userApi
    middleware:
      circuitBreaker:
        enabled: true
        failureThreshold: 30 # 50% failure rate threshold
        slowCallThreshold: 50 # 50% slow call rate threshold
        slowCallDuration: 500 # 0.5 seconds threshold for slow calls
        openStateDuration: 5 # 5-second wait in open state
        waitDurationInOpenState: 10000  # 10-second wait in open state
        permittedNumberOfCallsInHalfOpenState: 2 # 3 calls in half-open state
        minimumNumberOfCalls: 4 # Minimum of 4 calls before evaluating

```

## Explanation of Each Circuit Breaker State

#### **CLOSED**
- **Definition**: The system is operating normally, and all requests are allowed to pass through without restriction.
- **Behavior**:
  - Success, failure, and slow call rates are monitored.
  - If the failure or slow call rate exceeds the configured thresholds and the minimum number of calls is reached, the circuit transitions to the **OPEN** state.
- **Purpose**: This is the default state where the system allows full traffic while monitoring service health.

#### **OPEN**
- **Definition**: The system is in a "break" mode, meaning no requests are forwarded to the service to prevent further strain or cascading failures.
- **Behavior**:
  - All requests are rejected while the circuit is in this state.
  - The system waits for the configured `waitDurationInOpenState` before transitioning to the **HALF_OPEN** state.
- **Purpose**: Protects the system from being overwhelmed by requests when the service is unhealthy.


#### **HALF_OPEN**
- **Definition**: The system is in a testing phase, allowing a limited number of requests to determine if the service has recovered.
- **Behavior**:
  - A limited number of requests (configured by `permittedNumberOfCallsInHalfOpenState`) are allowed.
  - If these requests succeed, the circuit transitions to **CLOSED**.
  - If any request fails or is slow, the circuit transitions back to **OPEN**.
- **Purpose**: Gradually tests the service's health to determine if it can return to normal operation without fully opening the floodgates.

### State Transitions Summary

1. **From CLOSED to OPEN**: Triggered when failure or slow call rates exceed thresholds, based on the monitored metrics.
2. **From OPEN to HALF_OPEN**: Triggered after the configured wait duration (`waitDurationInOpenState`) expires.
3. **From HALF_OPEN to CLOSED**: Triggered when all permitted test requests are successful.
4. **From HALF_OPEN to OPEN**: Triggered when a test request fails or is slow.


This design ensures that:
- The system avoids overloading unhealthy services.
- Recovery is tested cautiously with minimal risk of regression.
- The state transitions are automated based on service health metrics.
