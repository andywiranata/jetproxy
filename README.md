
# JetProxy
![Overview!](https://jetproxy.andywiranata.me/assets/images/jetproxy-intro-3a53dc6772cf521d3d37312d672cf6f7.png "JetProxy Overview")

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/andywiranata/jetproxy)

**JetProxy** is a fast, lightweight, and flexible **reverse proxy** built using **Jetty**. Designed for developers and teams who need fine-grained control over proxy behavior, JetProxy is a developer-friendly alternative to tools like **Traefik** and **Nginx**.

[Documentation](https://jetproxy.andywiranata.me/docs/intro)

### Features

- 🌐 HTTP reverse proxy with dynamic routing
- 🔐 Middleware-based auth support (BasicAuth, JWT, ForwardAuth)
- 🧠 Flexible middleware engine for request handling and transformations:
  - Request rules, header manipulation, CORS, and rate limiting
  - Built-in caching, circuit breakers, and mirroring support
  - Experimental REST to gRPC conversion and idempotency handling
- ⚡ High-performance Jetty server core
- 📊 Per-route statistics (hits, status codes, latency, host breakdown)
- 💡 Declarative YAML-based configuration for routes and policies
- ☁️ Easy deployment as a standalone JAR – no external dependencies
- 🛠️ Built with modern Java 21 and Gradle

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
        mirrorService: userV2Api
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
export APP_CONFIG_PATH=/path/to/config.yaml
java -jar jetproxy-latest.jar
```
