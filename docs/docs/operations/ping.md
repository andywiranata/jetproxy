---
sidebar_position: 3
---

# PING

JetProxy provides a simple healthcheck endpoint to monitor the status of your proxy instance and its dependencies.

## Endpoint

```
GET /_jetproxy/healthcheck
```

This endpoint checks the liveness of JetProxy itself and the availability of connected services like Redis and upstream servers.

## Sample Response

```json
{
  "status": "UP",
  "redisStatus": "Healthy",
  "servers": {
    "http://localhost:30001/ping": "Not Found",
    "http://localhost:30002/v2/ping": "Unhealthy",
    "http://localhost:30003/ping": "Healthy"
  }
}
```

### Response Fields:

* `status`: Shows the status of JetProxy itself. Typically "UP" when the instance is running.
* `redisStatus`: Indicates the health of the Redis connection if Redis is configured.
* `servers`: A map of service URLs to their respective health status. Possible values include `Healthy`, `Unhealthy`, or `Not Found`.
