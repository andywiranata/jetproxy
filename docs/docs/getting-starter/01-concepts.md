---
sidebar_position: 1
---

# Concepts

### 1. Proxy
JetProxy core functionality is to act as a reverse proxy, forwarding client requests to backend services while abstracting the complexities of load balancing and traffic management.

Key Features:

* Forwarding HTTP(S) requests to target servers.
* Handle responses and send them back to clients.
* Support for both HTTP and HTTPS protocols.

### 2. HTTP Cache
JetProxy offers built-in caching mechanisms to reduce the load on backend services and speed up response times by serving frequently requested data from cache (e.g., Redis).

Key Features:

* Supports Redis as a backend for caching.
* Configurable cache expiration and TTL (Time-To-Live) for cached responses.
* Flexible caching rules based on URL patterns or query parameters.

Use Case:

* Caching API responses to avoid overloading your backend and reducing latency.
* Customizable TTL to define how long specific content should remain cached.

### 3. Role Authorization & Basic Authentication
JetProxy supports role-based authorization and basic authentication, enabling fine-grained access control for different users or clients.

Key Features:

* Basic HTTP authentication for restricted resources.
* Role-based authorization to control access to different services or routes based on user roles.
* Can integrate with external authentication services.


