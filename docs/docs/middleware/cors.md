---
sidebar_position: 7
---

# CORS Headers

CORS (Cross-Origin Resource Sharing) headers can be configured similarly to custom [headers](/docs/middleware/headers), providing a straightforward way to enhance security and manage cross-origin requests. When CORS headers are enabled, preflight requests are handled directly by the middleware, which generates and sends the appropriate response back to the client without forwarding the request to any service.

## Configuration Examples

```yaml
appName: ${JET_APP_NAME:API-PROXY}
port: ${JET_PORT:8080}
defaultTimeout: ${JET_DEFAULT_TIMEOUT:10000}
rootPath: ${JET_DASHBOARD:/}
debugMode: ${JET_DEBUG_MODE:true}
corsFilter:
  accessControlAllowMethods:
    - "*"
  accessControlAllowHeaders:
    - "*"
  accessControlAllowOriginList:
    - "*"
```

### accessControlAllowOriginList

The **accessControlAllowOriginList** defines the origins allowed to access resources. By default, it is set to *, which permits requests from all origins. This is generally not recommended as it can lead to security vulnerabilities.

```
accessControlAllowOriginList:
  - "http://example.com"
  - "https://sub.example.com"
  - "https?://*.example.[a-z]{2,3}"
```

* `http://example.com`: Allows requests from http://example.com.
* `https://sub.example.com`: Allows requests from https://sub.example.com.
* `https?://*.example.[a-z]{2,3}`: Matches http or https, subdomains of example.com, and top-level domains like .com, .org, etc.

When using * as the value:
```
accessControlAllowOriginList:
  - "*"
```
It permits all origins, which is suitable for public APIs but should be avoided for sensitive data.

### allowedMethods

The allowedMethods list specifies the HTTP methods permitted for resource access. By default, all commonly used HTTP methods are allowed:
```
allowedMethods:
  - "GET"
  - "POST"
```

To allow any method, use *:
```
allowedMethods:
  - "*"
```

### allowedHeaders

List of HTTP headers that are allowed to be specified when accessing the resources. Default value is X-Requested-With,Content-Type,Accept,Origin. If the value is a single "*", this means that any headers will be accepted. For example:

```
allowedHeaders:
  - "X-Requested-With"
  - "Content-Type"
  - "Accept"
  - "Origin"
```

**Note:**
CORS settings, such as `accessControlAllowOriginList`, `allowedMethods`, and `allowedHeaders`, are applied globally across the entire application. These configurations are not tied to specific services or proxies, meaning they affect all incoming requests uniformly. Be cautious when defining these settings, especially when using wildcards like *, to ensure they align with your application's security requirements.