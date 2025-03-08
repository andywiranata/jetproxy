---
sidebar_position: 1
---

# Quickstart
Get started with **JetProxy** in just a few simple steps. This guide will help you quickly set up proxies and services, so your API traffic is efficiently managed, secured, and ready to go.

### Step 1: Download JetProxy JAR

First, download the **JetProxy** JAR file to your local machine:

[download](https://github.com/andywiranata/jetproxy/releases) 

### Step 2: Create the Configuration File

Next, create a config.yaml file in the same directory where you downloaded the JAR file. This will define your proxy routes, services, and other settings.

```yaml
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

### Step 3: Run JetProxy

Now that you have the JAR and configuration set up, start JetProxy by running the following command:

```bash
java -jar jetproxy-latest.jar --config=config.yaml
```

Once JetProxy is running, you can access your services via the defined proxy routes:

* Access the httpbin service: http://localhost:8080/user

These routes will forward traffic to the respective backend services (httpbin.org and example.com).



