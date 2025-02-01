package io.jetproxy.util;

public class Constants {
    public static final String HEADER_RETRY_AFTER = "Retry-After";
    public static final String HEADER_X_PROXY_ERROR = "X-Proxy-Error";
    public static final String HEADER_X_PROXY_TYPE = "X-Proxy-Type";
    public static final String HEADER_X_JETPROXY_CACHE = "X-JetProxy-Cache";
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
    public static final String REQUEST_HEADER_USER_ID = "X-User-ID";
    public static final String REQUEST_HEADER_GRPC_SERVICE_NAME = "X-Grpc-Service-Name";
    public static final String REQUEST_HEADER_GRPC_METHOD_NAME = "X-Grpc-Method-Name";
    public static final String REQUEST_QUERY_GRPC_SERVICE_NAME = "grpc_service_name";
    public static final String REQUEST_QUERY_GRPC_METHOD_NAME = "grpc-method-Name";



}
