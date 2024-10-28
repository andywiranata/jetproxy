# Overview

In today's fast-paced microservices architecture, having an efficient and automated API gateway is essential. Managing the connection between your microservices and external users can be challenging, especially in dynamic environments where services are frequently added, removed, updated, or scaled.

Traditional reverse proxies demand manual configuration for every route, making them impractical in modern setups. This project introduces a **modern API reverse proxy**, designed to automate this process and dynamically route traffic to your microservices with minimal effort.

By automatically discovering services and adjusting routes, this reverse proxy simplifies management, enhances scalability, and reduces operational overhead, allowing you to focus on your services instead of maintaining complex routing configurations,

## Features (In Progress)

- **Dynamic Routing**: Automatically route traffic to microservices based on service discovery.
- **HTTP Caching**: Cache HTTP responses to reduce latency and improve performance.
- **Statistics**: Collect and display metrics for monitoring traffic and performance.
- **Basic Auth**
- **Sticky Session**
- **Rate Limiter**: Protect your services by limiting the number of requests per client.
- **Docker up and running**, externalize config

## Tech Stack

- **Language**: Java
- **Server**: Jetty
