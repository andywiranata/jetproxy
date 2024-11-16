---
sidebar_position: 4
---

# ForwardAuth

![alt text](forward-auth-middleware.png)

The `ForwardAuth` is a flexible `Authenticator` implementation designed for external authentication delegation. It allows forwarding specific request headers to an external service and processing the response headers dynamically.

[Learn more about header middleware actions](/docs/middleware/headers)

## Configuration Examples

### Example 1: Basic Forward Authentication

```yaml
service:
  - name: authApi
    url: http://localhost:30001
    methods: ['POST']
forwardAuth:
  service: authService
  path: /validate
  authRequestHeaders: |
    Forward(Authorization); 
    Forward(X-Custom-*)
  authResponseHeaders: |
    Forward(X-Auth-*)
```
Explanation
* Forwards Authorization and all headers starting with `X-Custom-`.
* Extracts all headers starting with `X-Auth-` from the response.

### Example 2: Advanced Header Manipulation

```yaml
service:
  - name: authApi
    url: http://localhost:30001
    methods: ['POST']
forwardAuth:
  service: authApi
  path: /verify
  authRequestHeaders: |
    Forward(X-Custom-*); 
    Copy(X-Trace-ID, X-New-); 
    Append(X-Request-ID, trace123); 
    Modify(User-Agent, Chrome, Firefox)
  authResponseHeaders: |
    Forward(X-Auth-*); 
    Copy(Set-Cookie, Custom-Cookie)

```

Explanation

Request Header Actions:
* Forward: Include all headers starting with `X-Custom-*`.
* Copy: Copy `X-Trace-ID` into a new namespace prefixed with `X-New-`.
* Append: Append `trace123` to `X-Request-ID`.
* Modify: Replace Chrome with Firefox in the `User-Agent` header.

Response Header Actions:
* Forward: Include all headers starting with `X-Auth-*`.
* Copy: Copy the `Set-Cookie` header into a new header prefixed with `Custom-Cookie`.