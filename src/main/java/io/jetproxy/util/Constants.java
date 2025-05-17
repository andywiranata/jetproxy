package io.jetproxy.util;

import java.time.Duration;
import java.util.List;

public class Constants {
    // Default Configurations
    public static final int DEFAULT_PORT = 80;
    public static final int DEFAULT_TIMEOUT = 10000;
    public static final int DEFAULT_MAX_AGE = 3600;
    public static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String UUID_PREFIX = "User";

    // CORS Defaults
    public static final List<String> DEFAULT_ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT"
    );
    public static final List<String> DEFAULT_ALLOWED_HEADERS = List.of("*");
    public static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of("*");

    // Circuit Breaker Defaults
    public static final boolean DEFAULT_CIRCUIT_BREAKER_ENABLED = false;
    public static final int DEFAULT_FAILURE_THRESHOLD = 50;
    public static final int DEFAULT_SLOW_CALL_THRESHOLD = 50;
    public static final int DEFAULT_SLOW_CALL_DURATION = 2000;
    public static final int DEFAULT_OPEN_STATE_DURATION = 10;
    public static final int DEFAULT_WAIT_DURATION_OPEN_STATE = 1000;
    public static final int DEFAULT_PERMITTED_CALLS_HALF_OPEN = 10;
    public static final int DEFAULT_MINIMUM_CALLS = 5;
    // Rate Limiter Defaults
    public static final boolean DEFAULT_RATE_LIMITER_ENABLED = false;
    public static final long DEFAULT_RATE_LIMIT_REFRESH_PERIOD = 1000;  // 1 second
    public static final int DEFAULT_RATE_LIMIT_FOR_PERIOD = 10;         // 10 requests per period
    public static final Duration DEFAULT_RATE_LIMIT_TIMEOUT = Duration.ZERO;
    public static final int DEFAULT_RATE_LIMIT_MAX_BURST_CAPACITY = 20; // Burst capacity of 20

    // gRPC Defaults
    public static final int DEFAULT_GRPC_PORT = 80;
    public static final List<String> DEFAULT_GRPC_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT"
    );

    // Middleware Idempotency Key
    public static final String DEFAULT_IDEMPOTENCY_KEY_HEADER_NAME = "Idempotency-Key";
    public static final long DEFAULT_IDEMPOTENCY_TTL = 5000;

    public static final String HEADER_RETRY_AFTER = "Retry-After";
    public static final String HEADER_X_PROXY_ERROR = "X-Proxy-Error";
    public static final String HEADER_X_PROXY_TYPE = "X-Proxy-Type";
    public static final String HEADER_X_JETPROXY_CACHE = "X-JetProxy-Cache";
    public static final String HEADER_X_JETPROXY_IDEMPOTENCY_CACHE = "X-JetProxy-Idempotency-Cache";
    public static final String HEADER_X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_X_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String TYPE_RATE_LIMITER = "rate-limiter";
    public static final String TYPE_CIRCUIT_BREAKER = "circuit-breaker";

    public static final String TYPE_BULKHEAD = "bulkhead";
    public static final String TYPE_METHOD_NOT_ALLOWED = "method-not-allowed";
    public static final String TYPE_GRPC_SERV0CE_METHOD_NOT_FOUND = "grpc-service-or-method-not-found";
    public static final String TYPE_RULE_NOT_ALLOWED = "rule-not-allowed";
    // Error Messages
    public static final String ERROR_METHOD_NOT_ALLOWED = "Method Not Allowed";
    public static final String ERROR_RULE_NOT_ALLOWED = "Rule not allowed processing request";

    public static final String REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE = "jetproxy-rewrite-service";
    public static final String REQUEST_ATTRIBUTE_JETPROXY_MIRRORING = "jetproxy-mirroring-service";
    public static final String REQUEST_ATTRIBUTE_JETPROXY_JWT_CLAIMS = "jetproxy-jwt-claims";
    public static final String REQUEST_ATTRIBUTE_JETPROXY_GRPC_SERVICE_NAME = "jetproxy-grpc-service-name";
    public static final String REQUEST_ATTRIBUTE_JETPROXY_GRPC_METHOD_NAME = "jetproxy-grpc-method-name";
    public static final String REQUEST_ATTRIBUTE_JETPROXY_TRACE_ID = "jetproxy-trace-id";
    public static final String REQUEST_HEADER_USER_ID = "X-User-ID";
    public static final String REQUEST_HEADER_GRPC_SERVICE_NAME = "X-Grpc-Service-Name";
    public static final String REQUEST_HEADER_GRPC_METHOD_NAME = "X-Grpc-Method-Name";
    public static final String REQUEST_QUERY_GRPC_SERVICE_NAME = "grpc_service_name";
    public static final String REQUEST_QUERY_GRPC_METHOD_NAME = "grpc-method-Name";



}
