---
sidebar_position: 6
---

# Service Match Rules

The "Service Match Rules" feature enables dynamic routing of requests to specific services based on defined match rules. The middleware evaluates each rule in the matches list, and the first matching rule determines the service to which the request is forwarded.

## Configuration Match

```yaml
proxies:
  - path: /user
    service: userApi
    matches:
      - rule: "Header('x-version', 'v2')"
        service: userV2Api
      - rule: "Header('x-version', 'v3')"
        service: userV3Api
    ttl: -1

services:
  - name: userApi
    url: http://localhost:30001
    methods: ['GET']
  - name: userV2Api
    url: http://localhost:30001/v2
    methods: [ 'GET' ]
  - name: userV3Api
    url: http://localhost:30001/v3
    methods: [ 'GET' ]
```

### How It Works

**Default Service:**
The service field (userApi in this example) acts as the fallback if no match rules are met.

**Match Rules:**
- The matches list contains rules evaluated sequentially.
- The first matching rule determines the target service.

**Fallback Behavior**
If no rules in the **matches** list are met, the request is routed to the default **service** defined in the proxy configuration.


