---
sidebar_position: 9
---

# Mirroring Request

When working on a new version of a microservice, refactoring code, or implementing risky changes, it can be too dangerous to deploy them live without careful testing. Issues could impact your end users if something goes wrong.

Request mirroring allows you to test new backends in production by sending them copies of traffic while ignoring their responses. By designating a backend as a shadow backend, JetProxy forwards requests to it along with the primary backend. However, the responses from shadow backends are ignored and never merged into the client’s response.

Mirroring traffic to your microservices enables you to test new backends in production while observing their behavior without affecting live users. For example, you can:

* **Testing New Backends:** Validate a new backend service with production traffic without affecting the user experience.
* **Performance Analysis:** Test how a new service handles production-like workloads.
* **Debugging and Monitoring:** Gather analytics or track anomalies in backend services.
* **Safe Migrations:** Compare the behavior of two backend services during migrations.

## Configuring Mirroring

```yaml

proxies:
  - path: /user
    service: userApi
    middleware:
        mirroring:
        enabled: true              # Enable or disable mirroring
        mirrorService: userV2Api   # Specify the service to mirror requests to
        mirrorPercentage: 100      # Percentage of requests to mirror (1-100)
services:
  - name: userApi
    url: http://localhost:30001
    methods: ['GET', 'POST']
  - name: userV2Api
    url: http://localhost:30001/v2
    methods: [ 'GET', 'POST']
```

> Calculate mirroring percentage using a unique user identifier. Prioritize the **"x-user-id"** header, and if it's empty, fall back to using the **SessionID** from HttpServletRequest.

## How It Works

* When a request arrives at JetProxy, the mirroring middleware checks if mirroring is enabled using the enabled parameter in the configuration.
* If enabled, it determines whether the request should be mirrored based on the **mirrorPercentage** value.
* The request is forwarded to the primary backend as usual, and a duplicate is sent asynchronously to the **mirrorService** backend.
* Responses from the **mirrorService** backend are discarded or logged for analysis.