const express = require('express');
const { NodeTracerProvider } = require('@opentelemetry/sdk-trace-node');
const { SimpleSpanProcessor } = require('@opentelemetry/sdk-trace-base');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-otlp-http');
const { Resource } = require('@opentelemetry/resources');
const { trace } = require('@opentelemetry/api');

process.env.OTEL_LOG_LEVEL = 'debug';  // Enable debug level logs for OpenTelemetry

// Initialize OpenTelemetry Tracer provider
const provider = new NodeTracerProvider({
    resource: new Resource({
        'service.name': 'forward-auth-service',  // Set the service name for the traces
    }),
});

// Set up the OTLP exporter to send traces to the OpenTelemetry Collector
const exporter = new OTLPTraceExporter({
    url: 'http://localhost:4318/v1/traces', // OTLP HTTP endpoint of OpenTelemetry Collector
});

// Add the exporter to the trace provider
provider.addSpanProcessor(new SimpleSpanProcessor(exporter));

// Register the provider
provider.register();

// Initialize Express
const app = express();
const PORT = process.env.PORT || 30001;

app.use(express.json());

// Middleware to create spans for each HTTP request
app.use((req, res, next) => {
    const span = trace.getTracer('default').startSpan(req.originalUrl);
    span.setAttribute('http.method', req.method);
    span.setAttribute('http.url', req.originalUrl);

    // End the span when the response is finished
    res.on('finish', () => {
        span.setAttribute('http.status_code', res.statusCode);
        span.end();
    });

    next();
});

// Route to simulate forward authentication
app.post('/verify', (req, res) => {
    const authHeader = req.headers['authorization'];
    console.log('auth::header::', req.headers);
    console.log('auth::header::', authHeader);

    // Check if the Authorization header is provided
    if (!authHeader) {
        return res.status(401).json({ error: 'Authorization header missing' });
    }
    // Token is present; respond with success
    res.status(200).json({
        message: 'Authentication successful',
        user: {
            id: 'user123',
            name: 'Mock User',
            roles: ['roleA', 'roleB'],
        }
    });
});

// Route to simulate fetching user details
app.get('/user', (req, res) =>{
    console.log('user::header::', req.headers);
    res.setHeader('X-Custom-Header', 'CustomValue');
    res.setHeader('X-Powered-By', 'Express with Love');
    res.status(200).json({
        user: {
            id: 'user123',
            name: 'Mock User',
            roles: ['roleA', 'roleB'],
        }
    });
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.status(200).send('Forward Auth Server is running');
});

// Start the server
app.listen(PORT, () => {
    console.log(`Forward Auth server running on port ${PORT}`);
});
