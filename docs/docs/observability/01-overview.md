---
sidebar_position: 1
---

# Overview

JetProxy's observability features provide comprehensive insights into your system's behavior and performance. These features include logs, access logs, metrics, and tracing, all of which can be configured globally or at more specific levels, such as per proxy or per service. This flexibility allows you to tailor observability settings to your application's needs, ensuring efficient debugging, monitoring, and optimization.

## Key Observability Features

### [Access Logs](/docs/observability/access-logs)
- Record details of incoming requests and outgoing responses.
- Includes information such as HTTP methods, paths, status codes, and response times.
- Useful for tracking traffic patterns and troubleshooting client-related issues.

### [Logs](/docs/observability/access-logs)
- Capture detailed information about system events, errors, and processes.
- Configurable log levels (e.g., INFO, DEBUG, ERROR) to control verbosity.
- Supports output to stdout, files, or external logging systems.

### [Metrics & Tracing (OpenTelemetry)](/docs/observability/metrics)
- Collect and expose performance metrics, such as request rates, latencies, and error rates.
- Trace requests as they move through the system, providing end-to-end visibility.
- Compatible with popular tracing tools like Newrelic.

These observability features enable you to monitor and maintain the health of your application effectively. Each feature can be customized to align with your operational requirements, ensuring a scalable and resilient system.

