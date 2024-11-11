---
sidebar_position: 3
---

# Routers

A router is responsible for directing incoming requests to the appropriate services that can process them. During this process, routers may apply various rules to validate the request or perform actions before passing the request along to the service.

### Configuring HTTP Routers

Service Mapping: Once the routing rules match a request, JetProxy forwards the request to the appropriate backend service, just like in the image where the routers forward to different services.

```yaml
proxies:
  # Router for handling API requests
  - path: /api
    service: apiService # Service Mapping
    rule: "Host(`api.example.com`) && PathPrefix(`/api`)"
    ttl: 30000  # Cache for 30 seconds

  # Router for handling static content
  - path: /static
    service: staticContentService
    rule: "Host(`static.example.com`) && Path(`/static`)"
    ttl: -1  # No caching for static content

  # Router for admin panel access, with additional authentication middleware
  - path: /admin
    service: adminService # Service Mapping
    rule: "Host(`admin.example.com`) && PathPrefix(`/admin`)"
    ttl: -1  # No caching for admin panel

  # Router for a specific API endpoint with custom headers validation
  - path: /secure-api
    service: secureApiService # Service Mapping
    rule: "Header(`X-Auth-Token`, `abc123`) && PathPrefix(`/secure`)"
    middleware: 'auth'

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

