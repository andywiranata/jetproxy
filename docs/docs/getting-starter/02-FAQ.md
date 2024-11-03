---
sidebar_position: 2
---

# FAQ



### What is JetProxy

JetProxy is a lightweight, high-performance HTTP proxy library built on Jetty. It is designed to handle advanced routing, caching, and security features for modern web applications. Whether you need an API gateway, traffic management, or load balancing, JetProxy simplifies the process with powerful built-in capabilities.

### What are the key features of JetProxy

JetProxy provides several core features to help developers manage HTTP traffic efficiently:

* Proxy: Routes HTTP requests from clients to backend services, acting as a reverse proxy.
* Caching: Built-in caching support with options for Redis or in-memory caching to speed up responses and reduce backend load.
* Role Authorization & Basic Authentication: Secure your services with role-based access control and basic HTTP authentication.
* Routing Rules: Customizable routing rules based on headers, URL paths, and query parameters for advanced traffic control.

### How does JetProxy handle security ?

JetProxy supports Role Authorization and Basic Authentication to control access to different endpoints or services. You can define user roles and restrict access to certain proxy paths using middleware. 

```yaml
proxies:
  - path: /admin
    service: adminApi
    middleware: 'basicAuth:administrator'
```

This ensures that only users with the **administrator** role can access the **/admin** route.

### What caching options are available?

* Redis Caching: Ideal for distributed environments where caching needs to be persistent and shared across instances.
* In-Memory Caching: Provides a faster, local cache with size and memory limits. This is great for applications where speed is critical and the cache doesn't need to be shared across servers.

Example of enabling Redis caching:

```yaml
storage:
  redis:
    enabled: true
    host: localhost
    port: 6379
    maxTotal: 128
```

You can also define a Time-To-Live (TTL) for cache entries. For instance, a TTL of 10000 means that the response is cached for 10 seconds:

```yaml
proxies:
  - path: /task
    service: tasksApi
    middleware: 'basicAuth:roleB'
    ttl: 10000
```

### How flexible are routing rules in JetProxy

JetProxy allows highly customizable routing rules. You can define routes based on URL paths, HTTP headers, and even more complex conditions like regex matches or header prefixes. For example, this rule forwards requests based on the User-Agent header and a custom header:

```yaml
proxies:
  - path: /products
    service: productApi
    rule: "(Header('Content-Type', 'application/json') && HeaderPrefix('User-Agent', 'Mozilla')) || HeaderRegex('X-Custom-Header', '^[a-zA-Z0-9]{10}$')"

```

### What are the typical use cases for JetProxy?

* **API Gateway**: Forward API requests to different microservices and handle complex traffic routing with caching and authentication.
* **Caching Layer**: Reduce load on backend services by caching frequently accessed resources, using either Redis or in-memory caching.
* **Security Gateway**: Add basic authentication and role-based access control for sensitive endpoints, ensuring only authorized users can access them.
* **Load Balancer**: Distribute traffic among backend services based on routing rules and manage high-load traffic efficiently.

