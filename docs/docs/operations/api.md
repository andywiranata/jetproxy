---
sidebar_position: 2
---
# API

# JetProxy API Reference

JetProxy provides admin endpoints for dynamic configuration and operational control. All endpoints are accessible under:

```
http://localhost:8080/_jetproxy/admin/config/
```

> All admin routes require **Basic Authentication**

### GET /_jetproxy/admin/config/proxies

Returns all configured proxy routes.

* **Auth**: Basic Auth  (Administrator)
* **Response**: JSON array of proxies.

### POST /_jetproxy/admin/config/proxies

Create or update a proxy route.

* **Auth**: Basic Auth (Administrator)
* **Content-Type**: `application/json`

<details>
<summary>📦 Sample Request Body</summary>

```json
{
  "path": "/user",
  "service": "userApi",
  "uuid": "dXNlckFwaS91c2Vy",
  "ttl": 50000,
  "middleware": {
    "basicAuth": "basicAuth:administrator",
    "forwardAuth": {
      "enabled": true,
      "path": "/verify",
      "service": "authApi",
      "requestHeaders": "Forward(X-Custom-*);Forward(Authorization);",
      "responseHeaders": "Remove(X-Powered-By)"
    },
    "rule": "",
    "mirroring": {
      "enabled": true,
      "mirrorService": "userV2Api",
      "mirrorPercentage": 100
    }
  }
}
```

</details>

### GET /_jetproxy/admin/config/services

List all backend services.

* **Auth**: Basic Auth (Administrator)
* **Response**: JSON array of services.

### POST /_jetproxy/admin/config/services

Create or update a backend service.

* **Auth**: Basic Auth (Administrator)
* **Content-Type**: `application/json`

<details>
<summary>📦 Sample Request Body</summary>

```json
{
  "path": "/user",
  "service": "userApi",
  "uuid": "dXNlckFwaS91c2Vy",
  "ttl": 50000,
  "middleware": {
    "basicAuth": "basicAuth:administrator",
    "forwardAuth": {
      "enabled": true,
      "path": "/verify",
      "service": "authApi",
      "requestHeaders": "Forward(X-Custom-*);Forward(Authorization);",
      "responseHeaders": "Remove(X-Powered-By)"
    },
    "rule": "Header('Content-Type', 'application/json')",
    "mirroring": {
      "enabled": true,
      "mirrorService": "userV2Api",
      "mirrorPercentage": 100
    }
  }
}
```

</details>

