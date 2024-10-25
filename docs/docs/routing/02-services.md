---
sidebar_position: 2
---

# Services

In JetProxy, a service is responsible for forwarding the incoming requests, after they have been processed by a router, to the appropriate backend system or application. Each service represents a specific backend target, such as an API, a web application, or a microservice, that handles the request and sends a response back to JetProxy, which then relays it to the client.

### Configuring Service Routers

```yaml
services:
  - name: apiService
    url: https://backend.example.com/api
    methods: ['GET', 'POST']

  - name: staticContentService
    url: https://static.example.com
    methods: ['GET']

  - name: adminService
    url: https://admin.example.com
    methods: ['GET', 'POST']

  - name: secureApiService
    url: https://secure.backend.example.com
    methods: ['GET', 'POST']
```
 * *Methods* : Services enforce which HTTP methods are allowed for each backend, ensuring proper operation based on the endpoint's capabilities.
