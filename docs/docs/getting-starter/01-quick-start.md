---
sidebar_position: 1
---

# Quickstart
Get started with **JetProxy** in just a few simple steps. This guide will help you quickly set up proxies and services, so your API traffic is efficiently managed, secured, and ready to go.

### Step 1: Download JetProxy JAR

First, download the **JetProxy** JAR file to your local machine:

```bash
curl -O https://example.com/jetproxy-latest.jar
```

### Step 2: Create the Configuration File

Next, create a config.yaml file in the same directory where you downloaded the JAR file. This will define your proxy routes, services, and other settings.

```yaml
appName: API-PROXY
port: 8080
rootPath: /

proxies:
  - path: /httpbin
    service: httpbinApi
    ttl: -1

services:
  - name: httpbinApi
    url: http://httpbin.org
    methods: ['GET', 'POST']
```

### Step 3: Run JetProxy

Now that you have the JAR and configuration set up, start JetProxy by running the following command:

```bash
java -jar jetproxy-latest.jar --config=config.yaml
```

Once JetProxy is running, you can access your services via the defined proxy routes:

* Access the httpbin service: http://localhost:8080/httpbin

These routes will forward traffic to the respective backend services (httpbin.org and example.com).



