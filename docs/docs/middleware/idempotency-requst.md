---
sidebar_position: 11
---

# Idempotency Request

Idempotency ensures that no matter how many times a client sends the same request, the server will perform the action only once—or return the same result without duplicating side effects.

## Configuration Idempotency Request

```yaml
proxies:
  - path: /upload
    service: fileUploadService
    middleware:
      idempotency:
        enabled: true
        headerName: Idempotency-Key  # Default header used to deduplicate
        ttl: 30000                   # Response cached for 30 seconds
      rule: "HeaderPrefix('Content-Type', 'multipart/form-data')"
```

### How It Works

With idempotency, clients can safely retry requests—such as creating a user or updating an order—without causing the operation to be executed more than once. Simply include a unique **Idempotency-Key** header with the request. If the request is retried (e.g., due to a timeout or network error), the server will recognize the key and return the original result instead of reprocessing it.

![Idempotency Request](https://www.andywiranata.me/static/27666184c9763286a928d075066183ce/906b5/idempotency-flow.png)

A typical approach:
* The client generates a unique key (e.g., UUID v4).
* The server caches the first response for that key—success or failure.
* Any subsequent request with the same key returns the cached response.