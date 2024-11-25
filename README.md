# Overview

In today's fast-paced microservices architecture, having an efficient and automated API gateway is essential. Managing the connection between your microservices and external users can be challenging, especially in dynamic environments where services are frequently added, removed, updated, or scaled.

Traditional reverse proxies demand manual configuration for every route, making them impractical in modern setups. This project introduces a **modern API reverse proxy**, designed to automate this process and dynamically route traffic to your microservices with minimal effort.

By automatically discovering services and adjusting routes, this reverse proxy simplifies management, enhances scalability, and reduces operational overhead, allowing you to focus on your services instead of maintaining complex routing configurations,

## Features (In Progress)

- **Dynamic Routing**: Automatically route traffic to microservices based on service discovery. [Link](https://jetproxy.andywiranata.me/docs/routing/overview)
- **HTTP Caching**: Cache HTTP responses to reduce latency and improve performance. [Link](https://jetproxy.andywiranata.me/docs/middleware/cache)
- **Statistics**: Collect and display metrics for monitoring traffic and performance. **TOOD - InReview to full utilize opentelemetry** 
- **Basic Auth**:  [Link](https://jetproxy.andywiranata.me/docs/middleware/basic-auth)
- **Forward Auth**: [Link](https://jetproxy.andywiranata.me/docs/middleware/forward-auth)
- **Rules**: [Link](https://jetproxy.andywiranata.me/docs/middleware/rules)
- **Headers**: [Link](https://jetproxy.andywiranata.me/docs/middleware/headers)
- **CircuitBreaker**: [Link](https://jetproxy.andywiranata.me/docs/middleware/circuit-breaker) **TODO**
- **Tracing OpenTelemetry**: **TODO**
- **Enabled Debug Logging**: **TODO**
- **Health Check: **TODO**
- **Sticky Session**: **TODO**
- **Rate Limiter**: Protect your services by limiting the number of requests per client. **TODO**
- **Docker up and running**, externalize config **TODO**

## Tech Stack

- **Language**: Java
- **Server**: Jetty
