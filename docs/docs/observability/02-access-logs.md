---
sidebar_position: 2
---

# Access Logs

The proxy supports structured and dynamic logging to facilitate debugging and observability. The logging behavior is influenced by the debugMode configuration.

##   Configuration 

The debugMode option is a boolean flag that enables or disables detailed logging for debugging purposes.

```yaml
appName: ${JET_APP_NAME:API-PROXY}
port: ${JET_PORT:8080}
defaultTimeout: ${JET_DEFAULT_TIMEOUT:10000}
rootPath: ${JET_DASHBOARD:/}
accessLog: ${JET_ACCESS_LOG:true}
```
**Log Format**
```
<remote_IP_address> - <client_user_name_if_available> [<timestamp>] "<request_method> <request_path><query_string_if_any> <request_protocol>" <HTTP_status> <content_length> "<request_referrer>" "<request_user_agent>" <response_time_in_ms>ms Cache: [<cache_status>] Status: [<status>]
```
**Log Output**

```
13:23:25.515 [qtp589987187-45] INFO  proxy.middleware.log.AccessLog - [0:0:0:0:0:0:0:1] [30/Nov/2024:13:23:25 +0700] "GET /user HTTP/1.1" TargetURL: [-] 200 70 "-" "Mozilla" [7ms] Cache: [true] Status: [Request processed]
```