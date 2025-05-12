package io.jetproxy.util;

/**
 * Centralized fatal error hints for configuration validation in JetProxy.
 */
public class FatalValidationHints {

    public static void missingAppName() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Missing 'appName' in config.")
                        .hint("Set a unique name for your JetProxy app.")
                        .example("appName: jetproxy-gateway")
                        .build()
        );
    }

    public static void invalidPort(int actualPort) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid port number: " + actualPort)
                        .hint("Port must be in range 1–65535.")
                        .example("port: 8080")
                        .build()
        );
    }

    public static void invalidTimeout(int actualTimeout) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid default timeout: " + actualTimeout)
                        .hint("Set a positive timeout value in milliseconds.")
                        .example("defaultTimeout: 10000")
                        .build()
        );
    }

    public static void invalidRootPath(String path) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid rootPath: " + path)
                        .hint("It must start with '/'.")
                        .example("rootPath: /")
                        .build()
        );
    }

    public static void missingCorsFilter() {
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
    }

    public static void missingCorsMethods() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowMethods' is missing.")
                        .hint("Defines allowed HTTP methods for cross-origin requests.")
                        .example("accessControlAllowMethods: [*]")
                        .doc("middleware/cors")
                        .build()
        );
    }

    public static void missingCorsOrigins() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowOriginList' is missing.")
                        .hint("List domains allowed to make cross-origin requests.")
                        .example("accessControlAllowOriginList: ['https://example.com']")
                        .doc("middleware/cors")
                        .build()
        );
    }

    public static void missingCorsHeaders() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowHeaders' is missing.")
                        .hint("Specify headers that clients may include in requests.")
                        .example("accessControlAllowHeaders: ['Content-Type', 'Authorization']")
                        .doc("middleware/cors")
                        .build()
        );
    }

    public static void missingUsername(String userRef) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User config: username is missing.")
                        .hint("Each user must have a unique username.")
                        .example("username: userA")
                        .doc("middleware/basic-auth")
                        .build()
        );
    }

    public static void missingPassword(String userRef) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User " + userRef + ": password is missing.")
                        .hint("Define a plaintext password or use secure storage.")
                        .example("password: secret123")
                        .doc("middleware/basic-auth")
                        .build()
        );
    }

    public static void missingRole(String userRef) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User " + userRef + ": role is missing.")
                        .hint("Assign a role that maps to basicAuth middleware.")
                        .example("role: admin")
                        .doc("middleware/basic-auth")
                        .build()
        );
    }
    public static void noServicesConfigured() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("No services configured.")
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
    }

    public static void missingServiceName() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Service name is missing.")
                        .hint("Each service must have a unique name.")
                        .example("name: my-service")
                        .doc("routing/services")
                        .build()
        );
    }

    public static void duplicateServiceName(String name) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Duplicate service name found: " + name)
                        .hint("Service names must be unique across the config.")
                        .doc("routing/services")
                        .build()
        );
    }

    public static void missingServiceUrl(String name) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Service URL is missing for service: " + name)
                        .hint("Each service must define a valid URL.")
                        .example("url: http://localhost:8080")
                        .doc("routing/services")
                        .build()
        );
    }

    public static void invalidServiceUrl(String url) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Service URL must start with 'http://' or 'https://': " + url)
                        .doc("routing/services")
                        .build()
        );
    }

    public static void emptyHttpMethods(String name) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("HTTP methods cannot be empty for service: " + name)
                        .hint("List the allowed HTTP methods like GET, POST, etc.")
                        .example("methods: ['GET', 'POST']")
                        .doc("routing/services")
                        .build()
        );
    }

    public static void invalidHttpMethod(String method, String serviceName) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid HTTP method '" + method + "' for service: " + serviceName)
                        .hint("Allowed methods are GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE, CONNECT.")
                        .build()
        );
    }

    public static void invalidHealthcheckPath(String path) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Healthcheck path must start with '/': " + path)
                        .build()
        );
    }
    public static void noProxiesConfigured() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("No proxies configured.")
                        .hint("At least one proxy route is required to forward traffic.")
                        .example("proxies:\n  - path: /api\n    service: apiService")
                        .doc("routing/routers")
                        .build()
        );
    }

    public static void noServicesForProxies() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("No services configured (HTTP or gRPC), but proxies depend on them.")
                        .hint("Ensure that either HTTP services or gRPC services are defined.")
                        .doc("routing/services")
                        .build()
        );
    }
    public static void proxyPathMissing() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Proxy path cannot be null or empty.")
                        .hint("Each proxy must define a valid route path.")
                        .example("path: /users")
                        .build()
        );
    }

    public static void proxyServiceMissing(String path) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Proxy service cannot be null or empty for path: " + path)
                        .hint("Each proxy must link to a defined service.")
                        .build()
        );
    }

    public static void proxyServiceUnregistered(String service, String path) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Proxy references an unregistered service: " + service + " for path: " + path)
                        .hint("Ensure the service name is defined in the services or grpcServices block.")
                        .build()
        );
    }

    public static void proxyPathMustStartWithSlash(String path) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Proxy path must start with '/': " + path)
                        .build()
        );
    }
}
