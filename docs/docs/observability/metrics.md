---
sidebar_position: 4
---

# Metrics

# OpenTelemetry

Integrate OpenTelemetry into the JetProxy project to enable distributed tracing, metrics collection, and logging for monitoring and observability of HTTP proxy activities.

**Auto Instrument**

Auto instrumentation simplifies adding observability to JetProxy by automatically capturing traces, metrics, and logs with minimal code changes. Using the OpenTelemetry Java agent, it enables seamless tracing for Jetty, monitors key metrics like latency and errors, and exports data to tools like Jaeger or Prometheus for quick analysis and insights.


## Configuration OpenTelemetry With NewRelic

Follow these steps to run JetProxy with OpenTelemetry Java Agent using the specified environment variables for New Relic integration.

### Prerequisites

1. **Download OpenTelemetry Java Agent**  
   Download the agent `.jar` file from the [OpenTelemetry GitHub Releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases).

2. **Ensure New Relic API Key**  
   Obtain your API key from your New Relic account.

#### Steps to Run

#### 1. Set Environment Variables

Define the required environment variables in your terminal or deployment environment:
```bash
export OTEL_SERVICE_NAME=JetProxy
export OTEL_EXPERIMENTAL_EXPORTER_OTLP_RETRY_ENABLED=true
export OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION=BASE2_EXPONENTIAL_BUCKET_HISTOGRAM
export OTEL_EXPERIMENTAL_RESOURCE_DISABLED_KEYS=process.command_args
export OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp.nr-data.net
export OTEL_EXPORTER_OTLP_HEADERS=api-key=<YOUR_API_KEY>
export OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT=4095
export OTEL_EXPORTER_OTLP_COMPRESSION=gzip
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
export OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE=delta
```

Replace `<YOUR_API_KEY>` with your actual New Relic API key.


#### 2. Run the Application
Use the java command to start your application with the OpenTelemetry agent:

```bash
java -javaagent:/path/to/opentelemetry-javaagent.jar \
     -jar /path/to/your-application.jar
```

### 3. Run with `gradlew run`

You can run JetProxy with OpenTelemetry integration using the `gradlew run` task, along with environment variables for New Relic configuration.

**Prerequisites: Clone the Repository**  
Clone the JetProxy repository:
```bash
git clone https://github.com/andywiranata/jetproxy
cd jetproxy
```