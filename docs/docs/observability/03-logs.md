---
sidebar_position: 3
---

# Logs

JetProxy uses **Logback** as the underlying logging framework. **Logback** is a powerful and flexible logging system, widely used for its performance and configurability. By default, JetProxy writes logs to stdout in text format. The default configuration provides simplicity and works well for development environments without additional setup.

[logback docs](https://logback.qos.ch/manual/introduction.html)

## Default Settings

1. Root Logger Level: **INFO**
    * This means only **INFO**, **WARN**, and **ERROR** messages are logged by default.
2. Appender: **ConsoleAppender**
    * Logs are written to the standard output (console).
    * The default log format is:
    ```
    %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
    ```
    Example:
    ```
    2025-01-24 13:30:19 INFO io.jetproxy.MainProxy - JetProxy server started on port 8080
    ```
    
# Configuring Logs

To dynamically set logging configuration via a YAML file, add:

```yaml
logging:
  root:
    level: INFO
  appenders:
    - name: STDOUT
      className: "ch.qos.logback.core.ConsoleAppender"
      encoder:
        pattern: "%d{yyyy-MM-dd HH:mm:ss} %-1level %logger{36} - %msg%n"

```
