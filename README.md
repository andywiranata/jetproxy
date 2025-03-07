
# JetProxy
![Overview!](https://jetproxy.andywiranata.me/assets/images/jetproxy-intro-3a53dc6772cf521d3d37312d672cf6f7.png "JetProxy Overview")

**JetProxy** is a lightweight, high-performance HTTP proxy library built with **Jetty**. It is designed for developers seeking a flexible and efficient solution for routing, caching, and managing HTTP requests.

[Documentation](https://jetproxy.andywiranata.me/docs/intro)

### Key Features
- **Dynamic Routing**  
  Advanced routing rules based on headers, query parameters, paths, and more, allowing precise traffic management without complex scripts.
- **Customizable Middleware**  
  Pre-built middleware for tasks like authentication, rate limiting, header manipulation, and caching to streamline HTTP request handling.
- **High Performance**  
  Built on Jetty, JetProxy is designed for low-latency, high-throughput environments, ensuring reliability even under heavy traffic.
- **Resilience**  
  JetProxy ensures reliability and fault tolerance with built-in mechanisms such as:
  - **Circuit Breaker**: Protects your system from cascading failures by halting traffic to unhealthy services and resuming only when stability is restored.
  - **Rate Limiter**: Controls traffic flow to prevent overloading backend services, handling spikes gracefully and maintaining optimal performance.
  - **HTTP Caching**: Reduces response times and backend load by caching frequently accessed resources using in-memory or Redis-based caching.

### Enhanced Security
JetProxy ensures your API is secure with robust authentication capabilities, including support for external authentication services. The `ForwardAuth` middleware allows seamless integration with external authentication mechanisms, enabling token validation and role-based access without tying security to a single system. This flexibility helps meet diverse security needs.

### Simplicity and Efficiency
JetProxy eliminates the need for constant manual updates by offering straightforward YAML configuration and real-time adjustments. Its intuitive design reduces operational complexity, enabling teams to focus on service development rather than maintaining intricate proxy configurations.

# How to Run
## Step 1: Download Latest Jar
Download latest [jar](https://github.com/andywiranata/jetproxy/releases) 
## Step 2: Create the Configuration File
Create file **config.yaml**
```
appName: ${JET_APP_NAME:API-PROXY}
port: ${JET_PORT:8080}
defaultTimeout: ${JET_DEFAULT_TIMEOUT:10000}
dashboard: ${JET_DASHBOARD:true}
rootPath: ${JET_DASHBOARD:/}
accessLog: ${JET_DEBUG_MODE:true}

corsFilter:
  accessControlAllowMethods:
    - "*"
  accessControlAllowHeaders:
    - "*"
  accessControlAllowOriginList:
    - "*"
  maxAge: 3600

storage:
  inMemory:
    enabled: true
    maxMemory: 50 # MB
    size: 10000

proxies:
  - path: /upload
    service: fileUploadService
    middleware:
      rule: "HeaderPrefix('Content-Type', 'multipart/form-data')"
  - path: /v2/grpc
    service: userGrpcApi
    ttl: 10000
  - path: /user
    service: userApi
    matches:
      - rule: "Header('Content-Type', 'application/json')"
        service: userApi
      - rule: "Header('x-header-hello', 'x-header-hello')"
        service: userApi
    middleware:
      basicAuth: 'basicAuth:administrator'
      mirroring:
        enabled: true
        mirrorService: userV2xApi
        mirrorPercentage: 100
      forwardAuth:
        enabled: false
        path: /verify
        service: authApi
        requestHeaders: "Forward(X-Custom-*);Forward(Authorization);"
        responseHeaders: "Remove(X-Powered-By)"
    ttl: 1000
services:
  - name: userApi
    url: http://localhost:30001
    methods: ['GET', 'POST']
  - name: userV2Api
    url: http://localhost:30001/v2
    methods: [ 'GET', 'POST']
  - name: authApi
    url: http://localhost:30001
    methods: ['POST']
  - name: fileUploadService
    url: http://localhost:30001
    methods: ['POST']

grpcServices:
  - name: userGrpcApi
    host: localhost
    port: 50051

users:
  - username: admin
    password: admin
    role: administrator

```
## Step 3: Run
```
java -jar jetproxy-latest.jar --config=config.yaml
```
