package io.jetproxy.util;

import io.jetproxy.exception.JetProxyValidationException;

/**
 * Centralized fatal error hints for configuration validation in JetProxy.
 */
public class FatalValidationHints {

    public static void missingAppName() {
        final String msg = "Missing 'appName' in config.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set a unique name for your JetProxy app.")
                        .example("appName: jetproxy-gateway")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidPort(int actualPort) {
        final String msg = "Invalid port number: " + actualPort;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Port must be in range 1–65535.")
                        .example("port: 8080")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidTimeout(int actualTimeout) {
        final String msg = "Invalid default timeout: " + actualTimeout;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set a positive timeout value in milliseconds.")
                        .example("defaultTimeout: 10000")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidRootPath(String path) {
        final String msg = "Invalid rootPath: " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid rootPath: " + path)
                        .hint("It must start with '/'.")
                        .example("rootPath: /")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsFilter() {
        final String msg = "Missing CORS filter config.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Missing CORS filter config.")
                        .hint("Define global CORS settings under `corsFilter:` block.")
                        .example("""
                    corsFilter:
                      accessControlAllowMethods: ['GET', 'POST']
                      accessControlAllowHeaders: ['*']
                      accessControlAllowOriginList: ['*']
                    """)
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsMethods() {
        final String msg = "CORS: 'accessControlAllowMethods' is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Defines allowed HTTP methods for cross-origin requests.")
                        .example("accessControlAllowMethods: [*]")
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsOrigins() {
        String msg = "CORS: 'accessControlAllowOriginList' is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowOriginList' is missing.")
                        .hint("List domains allowed to make cross-origin requests.")
                        .example("accessControlAllowOriginList: ['https://example.com']")
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsHeaders() {
        String msg = "CORS: 'accessControlAllowHeaders' is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowHeaders' is missing.")
                        .hint("Specify headers that clients may include in requests.")
                        .example("accessControlAllowHeaders: ['Content-Type', 'Authorization']")
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingUsername() {
        final String msg = "User config: username is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each user must have a unique username.")
                        .example("username: userA")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingPassword(String userRef) {
        final String msg = "User " + userRef + ": password is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Define a plaintext password or use secure storage.")
                        .example("password: secret123")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingRole(String userRef) {
        final String msg = "User " + userRef + ": role is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User " + userRef + ": role is missing.")
                        .hint("Assign a role that maps to basicAuth middleware.")
                        .example("role: admin")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void noServicesConfigured() {
        final String msg = "No services configured.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("You must define at least one HTTP service.")
                        .example("""
                                services:
                                  - name: userApi
                                    url: http://localhost:30001
                                    methods: ['GET', 'POST']
                                    healthcheck: /ping
                                """)
                .doc("routing/services")
                .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingServiceName() {
        final String msg = "Service name is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each service must have a unique name.")
                        .example("name: my-service")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void duplicateServiceName(String name) {
        final String msg = "Duplicate service name found: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Service names must be unique across the config.")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingServiceUrl(String name) {
        final String msg = "Service URL is missing for service: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each service must define a valid URL.")
                        .example("url: http://localhost:8080")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidServiceUrl(String url) {
        final String msg = "Service URL must start with 'http://' or 'https://': " + url;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void emptyHttpMethods(String name) {
        final String msg = "HTTP methods cannot be empty for service: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("HTTP methods cannot be empty for service: " + name)
                        .hint("List the allowed HTTP methods like GET, POST, etc.")
                        .example("methods: ['GET', 'POST']")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidHttpMethod(String method, String serviceName) {
        final String msg = "Invalid HTTP method '" + method + "' for service: " + serviceName;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Allowed methods are GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE, CONNECT.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidHealthcheckPath(String path) {
        final String msg = "Healthcheck path must start with '/': " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void noProxiesConfigured() {
        final String msg = "No proxies configured.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg
                        )
                        .hint("At least one proxy route is required to forward traffic.")
                        .example("proxies:\n  - path: /api\n    service: apiService")
                        .doc("routing/routers")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void noServicesForProxies() {
        final String msg = "No services configured (HTTP or gRPC), but proxies depend on them.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Ensure that either HTTP services or gRPC services are defined.")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void proxyPathMissing() {
        final String msg = "Proxy path cannot be null or empty.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Proxy path cannot be null or empty.")
                        .hint("Each proxy must define a valid route path.")
                        .example("path: /users")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void proxyServiceMissing(String path) {
        final String msg = "Proxy service cannot be null or empty for path: " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each proxy must link to a defined service.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void proxyServiceUnregistered(String service, String path) {
        final String msg = "Proxy references an unregistered service: " + service + " for path: " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Ensure the service name is defined in the services or grpcServices block.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void proxyPathMustStartWithSlash(String path) {
        final String msg = "Proxy path must start with '/': " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void matchRuleMissing(String proxyPath) {
        final String msg = "Match rule cannot be null or empty in proxy: " + proxyPath;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Match rule cannot be null or empty in proxy: " + proxyPath)
                        .hint("Each match must specify a valid rule condition.")
                        .doc("middleware/service-match")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void matchServiceMissing(String proxyPath) {
        final String msg = "Match service cannot be null or empty in proxy: " + proxyPath;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Match service cannot be null or empty in proxy: " + proxyPath)
                        .hint("Each match must link to a defined service.")
                        .doc("middleware/service-match")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void matchServiceUnregistered(String matchService, String proxyPath) {
        final String msg = "Match references an unregistered service: " + matchService + " in proxy: " + proxyPath;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Match references an unregistered service: " + matchService + " in proxy: " + proxyPath)
                        .hint("Ensure the match service name exists in the HTTP or gRPC service definitions.")
                        .doc("middleware/service-match")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void grpcServiceNameMissing() {
        final String msg = "GrpcService name cannot be null or empty.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each gRPC service must have a unique and non-empty name.")
                        .example("name: my-grpc-service")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void duplicateGrpcServiceName(String name) {
        final String msg = "Duplicate GrpcService name found: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each gRPC service must have a unique name.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void grpcServiceHostMissing(String serviceName) {
        final String msg = "GrpcService Host cannot be null or empty for service: " + serviceName;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify a reachable host name or IP for the gRPC service.")
                        .example("host: localhost")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void grpcServicePortMissing(String serviceName) {
        final String msg = "GrpcService Port cannot be null or empty for service: " + serviceName;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify a valid port number for the gRPC service.")
                        .example("port: 50051")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void basicAuthRolesMissing() {
        final String msg = "BasicAuth middleware is enabled but has no roles specified.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("BasicAuth middleware is enabled but has no roles specified.")
                        .hint("Specify at least one role when enabling BasicAuth.")
                        .example("basicAuth: admin")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void forwardAuthPathMissing() {
        final String msg = "ForwardAuth middleware is enabled but path is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify the auth endpoint path, e.g., /auth/verify.")
                        .example("path: /auth/verify")
                        .doc("middleware/forward-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void forwardAuthServiceMissing() {
        final String msg = "ForwardAuth middleware is enabled but service is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify the name of the service that handles external authentication.")
                        .example("service: authService")
                        .doc("middleware/forward-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void forwardAuthServiceUnregistered(String serviceName) {
        final String msg = "ForwardAuth middleware references an unregistered service: '" + serviceName + "'.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Ensure the service is declared in the `services:` block.")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void forwardAuthRequestHeadersMissing() {
        final String msg = "ForwardAuth middleware is enabled but requestHeaders are missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("At least one request header must be forwarded to the auth service.")
                        .example("authRequestHeaders: Forward(Authorization)")
                        .doc("middleware/forward-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void forwardAuthResponseHeadersMissing() {
        final String msg = "ForwardAuth middleware is enabled but responseHeaders are missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Define how auth response headers should be handled.")
                        .example("authResponseHeaders: Forward(X-Auth-*)")
                        .doc("middleware/forward-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void rateLimiterInvalidRefreshPeriod() {
        final String msg = "RateLimiter is enabled but limitRefreshPeriod is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify a positive value for the token bucket refill period (in milliseconds).")
                        .example("limitRefreshPeriod: 1000")
                        .doc("middleware/rate-limiter")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void rateLimiterInvalidLimitForPeriod() {
        final String msg = "RateLimiter is enabled but limitForPeriod is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify how many tokens to add during each refill period.")
                        .example("limitForPeriod: 10")
                        .doc("middleware/rate-limiter")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void rateLimiterInvalidBurstCapacity() {
        final String msg = "RateLimiter is enabled but maxBurstCapacity is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify the maximum number of tokens the bucket can hold.")
                        .example("maxBurstCapacity: 20")
                        .doc("middleware/rate-limiter")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void circuitBreakerInvalidFailureThreshold() {
        final String msg = "CircuitBreaker is enabled but failureThreshold is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set failureThreshold between 1 and 100.")
                        .example("failureThreshold: 50")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void circuitBreakerInvalidSlowCallThreshold() {
        final String msg = "CircuitBreaker is enabled but slowCallThreshold is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set slowCallThreshold between 1 and 100.")
                        .example("slowCallThreshold: 50")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void circuitBreakerInvalidSlowCallDuration() {
        final String msg = "CircuitBreaker is enabled but slowCallDuration is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set a positive duration in milliseconds for slow call threshold.")
                        .example("slowCallDuration: 2000")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void circuitBreakerInvalidOpenStateDuration() {
        final String msg = "CircuitBreaker is enabled but openStateDuration is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify a positive duration the circuit remains open.")
                        .example("openStateDuration: 10")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void circuitBreakerInvalidWaitDuration() {
        final String msg = "CircuitBreaker is enabled but waitDurationInOpenState is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set how long the circuit waits before testing again.")
                        .example("waitDurationInOpenState: 10000")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void circuitBreakerInvalidHalfOpenCalls() {
        final String msg = "CircuitBreaker is enabled but permittedNumberOfCallsInHalfOpenState is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify how many test calls are allowed in half-open state.")
                        .example("permittedNumberOfCallsInHalfOpenState: 3")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void circuitBreakerInvalidMinimumCalls() {
        final String msg = "CircuitBreaker is enabled but minimumNumberOfCalls is invalid.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify the minimum number of calls before the breaker evaluates health.")
                        .example("minimumNumberOfCalls: 5")
                        .doc("middleware/circuit-breaker")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void headerRequestHeadersMissing() {
        final String msg = "Header middleware is enabled but requestHeaders are missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Define how incoming request headers should be transformed or filtered.")
                        .example("requestHeaders: Remove(Authorization)")
                        .doc("middleware/headers")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void headerResponseHeadersMissing() {
        final String msg = "Header middleware is enabled but responseHeaders are missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Define how outgoing response headers should be transformed or added.")
                        .example("responseHeaders: Add(X-Powered-By, jetty-server)")
                        .doc("middleware/headers")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void idempotencyHeaderMissing() {
        final String msg = "Idempotency middleware is enabled but header name is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify which header to use for idempotency key tracking.")
                        .example("headerName: Idempotency-Key")
                        .doc("middleware/idempotency")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void mirroringServiceMissing() {
        final String msg = "Mirroring middleware is enabled but service is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Specify a target service to mirror traffic to.")
                        .example("mirrorService: audit-service")
                        .doc("middleware/mirroring")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

}
